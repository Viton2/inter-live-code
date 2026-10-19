# Registro de uso de IA

Ferramenta: **Claude Code** (modelo Claude Opus 5.5), na extensão do VS Code.

## Prompts enviados

1. As regras do desafio (contract-first, borda assíncrona, testes unitários e de integração obrigatórios,
   1h30, uso de IA registrado no projeto).
2. O enunciado "SPP — Sistema de Pagamento com Porquinhos" (três serviços: PiggiesPaymentCoordinator,
   PiggiesReserveService e PiggiesMerchantService).

## O que a IA fez

1. Analisou o esqueleto Micronaut 5.1.5 (Postgres, Kafka e Testcontainers já configurados).
2. Escreveu os contratos primeiro: um OpenAPI por serviço em `contracts/openapi` e um JSON Schema por evento
   em `contracts/events`.
3. Implementou os três serviços como pacotes isolados que só se comunicam via Kafka.
4. Escreveu os testes unitários e de integração e os rodou até passarem (31 testes).

## Problemas encontrados no caminho

- **O build falhava no esqueleto original**, com `TypeNotPresentException: TestResourcesLifecycleExtension`.
  A causa era o `mvnw` rodando com o Java 21 do IntelliJ (`JAVA_HOME`), enquanto o projeto exige o Java 25.
  A correção foi exportar `JAVA_HOME` apontando para um JDK 25.
- **O `application.yml` estava com a senha do banco vazia**, diferente do `docker-compose.yml`
  (`postgres`). Foi corrigido.
- **O `assertThat` ficava ambíguo com `HttpStatus`**, porque `HttpStatus` do Micronaut implementa
  `CharSequence`. Os testes passaram a comparar o código numérico.

## Decisões de design (e por quê)

- **Coreografia + orquestração leve:** o Coordinator publica um único evento `payment.initiated`, consumido
  em paralelo por Reserve e Merchant (consumer groups diferentes). Isso cumpre o "em paralelo" do enunciado
  sem chamadas síncronas entre serviços.
- **Um evento `payment.confirmed` dispara o débito e o crédito:** o mesmo evento serve aos dois serviços e a
  "outros sistemas", como pede a etapa 3 do enunciado.
- **Lock pessimista na linha do pagamento:** os resultados de reserva e de merchant chegam por tópicos
  diferentes e podem ser processados ao mesmo tempo. Sem lock, os dois poderiam ver o outro como `PENDING` e
  o pagamento nunca seria confirmado.
- **Um deployable só:** a escolha foi pelo tempo disponível. O README explica como os limites entre
  serviços foram preservados.

Não foi usado código de terceiros além das dependências declaradas no `pom.xml`.
