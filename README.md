# SPP — Sistema de Pagamento com Porquinhos

Pagamento em Piggies (stablecoin do Inter) via QR Code, com três serviços:
**PiggiesPaymentCoordinator**, **PiggiesReserveService** e **PiggiesMerchantService**.

## Contratos (contract-first)

| Serviço | Contrato HTTP | Eventos publicados |
|---|---|---|
| PiggiesPaymentCoordinator | [piggies-payment-coordinator.yaml](contracts/openapi/piggies-payment-coordinator.yaml) | `piggies.payment.initiated`, `piggies.payment.confirmed`, `piggies.payment.rejected` |
| PiggiesReserveService | [piggies-reserve-service.yaml](contracts/openapi/piggies-reserve-service.yaml) | `piggies.reserve.result` |
| PiggiesMerchantService | [piggies-merchant-service.yaml](contracts/openapi/piggies-merchant-service.yaml) | `piggies.merchant.validation.result` |

Os schemas dos eventos (JSON Schema 2020-12) ficam em [contracts/events](contracts/events). Os records em
`co.inter.piggies.events` espelham esses schemas. Todo evento usa o `paymentId` como chave Kafka, o que mantém
a ordem por pagamento dentro da partição.

## Fluxo

```
App ──POST /payments──▶ Coordinator ── grava intenção (PENDING) ──▶ 202 Accepted + Location
                             │
                             └─▶ piggies.payment.initiated ──┬─▶ ReserveService  (reserva saldo)
                                                             └─▶ MerchantService (valida merchant)
                                         ◀── piggies.reserve.result ─────────┘
                                         ◀── piggies.merchant.validation.result
Coordinator (os dois OK) ─▶ piggies.payment.confirmed ──┬─▶ ReserveService  (debita cliente)
                                                        ├─▶ MerchantService (credita recebível)
                                                        └─▶ outros sistemas
Coordinator (alguma falha) ─▶ piggies.payment.rejected ──▶ ReserveService (libera reserva)

App ──GET /payments/{id}──▶ PENDING | CONFIRMED | REJECTED (+ reason)
```

- **Borda assíncrona:** `POST /payments` só valida o QR Code, grava a intenção e responde `202`. O cliente
  não fica esperando a ampulheta e acompanha o status por polling no `Location`.
- **Paralelismo:** reserva e validação do merchant consomem o mesmo evento em consumer groups distintos, ao
  mesmo tempo. Os resultados chegam em qualquer ordem, e o `Payment` (uma máquina de estados) decide quando confirmar.
- **Concorrência:** os dois resultados podem chegar juntos, então o Coordinator trava a linha do pagamento
  (`PESSIMISTIC_WRITE`). O ReserveService trava conta e reserva da mesma forma.
- **Idempotência:** um evento repetido não altera o estado. Há uma reserva por `paymentId`, um recebível por
  `paymentId`, e resultados duplicados são ignorados.
- **Reserva órfã:** se o merchant for invalidado antes de a reserva concluir, o Coordinator reenvia
  `payment.rejected` quando a reserva chega, e o ReserveService libera o valor.

QR Code: `piggies://pay?merchant={merchantId}&amount={piggies}`. O valor é sempre um inteiro maior que 0.

Motivos de rejeição: `INSUFFICIENT_BALANCE`, `ACCOUNT_NOT_FOUND`, `MERCHANT_NOT_FOUND`, `MERCHANT_INACTIVE`.

## Decisão de empacotamento

Pelo tempo do desafio (1h30), os três serviços estão em **um único deployable**, isolados por pacote
(`coordinator`, `reserve`, `merchant`). Mesmo assim, eles se comportam como microsserviços:

- nenhum pacote importa classes de outro (exceto os DTOs de evento em `events`, que espelham os contratos);
- toda comunicação entre serviços passa pelo Kafka, cada serviço com seu próprio consumer group e Kafka client;
- cada serviço é dono das suas tabelas (prefixos `coordinator_`, `reserve_`, `merchant_`).

Separar em três aplicações exige só mover cada pacote para um módulo Maven com seu próprio `Application`.

## Como rodar

Requer **Java 25** e Docker. Se o `JAVA_HOME` apontar para outra versão, o plugin do Micronaut falha com
`TypeNotPresentException: TestResourcesLifecycleExtension`.

```bash
docker compose up -d
./mvnw mn:run
```

Exemplo:

```bash
curl -X POST localhost:8080/accounts/cliente-a/deposits -H 'Content-Type: application/json' -d '{"amount":500}'
curl -X POST localhost:8080/merchants -H 'Content-Type: application/json' -d '{"merchantId":"merchant-x","name":"Loja X","active":true}'
curl -i -X POST localhost:8080/payments -H 'Content-Type: application/json' \
     -d '{"customerId":"cliente-a","qrCode":"piggies://pay?merchant=merchant-x&amount=100"}'
curl localhost:8080/payments/{paymentId}           # CONFIRMED
curl localhost:8080/accounts/cliente-a             # balance 400
curl localhost:8080/merchants/merchant-x/receivables
```

## Testes

```bash
./mvnw test
```

- **Unitários:** `QrCodeTest`, `PaymentTest` (máquina de estados), `AccountTest` (reserva, débito e liberação)
  e `MerchantValidationTest`.
- **Integração** (Testcontainers com Postgres e Kafka reais): `PaymentFlowIntegrationTest` cobre o pagamento
  confirmado, o saldo insuficiente, o merchant inativo (com liberação da reserva), o QR Code inválido (400) e o
  pagamento inexistente (404).

## Limitações conhecidas e próximos passos

- **Dual write:** o evento é publicado dentro da transação do banco. Se o commit falhar depois do envio, o
  evento sai duplicado (mitigado pela idempotência dos consumidores). O ideal é usar um **Transactional Outbox**.
- Não há timeout para pagamentos `PENDING`, por exemplo quando um serviço fica fora do ar. Falta um job de
  expiração que rejeite o pagamento e libere a reserva.
- Não há header `Idempotency-Key` no `POST /payments`, então um retry do App cria outro pagamento.
- Os schemas não são validados em runtime (o ideal é um Schema Registry).
- Falta DLQ e uma política de retry para mensagens com erro.

## Uso de IA

Veja [docs/ai/PROMPTS.md](docs/ai/PROMPTS.md).
