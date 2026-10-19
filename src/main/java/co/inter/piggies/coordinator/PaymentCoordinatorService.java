package co.inter.piggies.coordinator;

import co.inter.piggies.events.MerchantValidationResult;
import co.inter.piggies.events.PaymentConfirmed;
import co.inter.piggies.events.PaymentInitiated;
import co.inter.piggies.events.PaymentRejected;
import co.inter.piggies.events.ReserveResult;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class PaymentCoordinatorService {

    private final PaymentRepository repository;
    private final EntityManager entityManager;
    private final CoordinatorEventPublisher publisher;

    /** Registra a intencao e dispara, em paralelo, reserva e validacao do merchant. */
    @Transactional
    public Payment initiate(String customerId, QrCode qrCode) {
        Payment payment = repository.save(Payment.initiate(customerId, qrCode));
        publisher.initiated(payment.getId(), new PaymentInitiated(UUID.randomUUID(), Instant.now(),
                payment.getId(), payment.getCustomerId(), payment.getMerchantId(), payment.getAmount()));
        log.info("Pagamento {} iniciado: {} -> {} ({} Piggies)", payment.getId(),
                payment.getCustomerId(), payment.getMerchantId(), payment.getAmount());
        return payment;
    }

    @Transactional
    public Optional<Payment> find(UUID paymentId) {
        return repository.findById(paymentId);
    }

    @Transactional
    public void onReserveResult(ReserveResult result) {
        lock(result.paymentId()).ifPresent(payment -> {
            boolean reserved = result.status() == ReserveResult.Status.RESERVED;
            publish(payment, payment.onReserveResult(reserved, result.reason()));
            if (reserved && payment.hasOrphanReservation()) {
                // merchant ja havia sido invalidado: reenvia a rejeicao para o ReserveService liberar
                publishRejected(payment);
            }
        });
    }

    @Transactional
    public void onMerchantResult(MerchantValidationResult result) {
        lock(result.paymentId()).ifPresent(payment -> publish(payment,
                payment.onMerchantResult(result.status() == MerchantValidationResult.Status.VALID, result.reason())));
    }

    /** Lock pessimista: os dois resultados podem chegar ao mesmo tempo em threads diferentes. */
    private Optional<Payment> lock(UUID paymentId) {
        Payment payment = entityManager.find(Payment.class, paymentId, LockModeType.PESSIMISTIC_WRITE);
        if (payment == null) {
            log.warn("Resultado para pagamento desconhecido {}", paymentId);
        }
        return Optional.ofNullable(payment);
    }

    private void publish(Payment payment, Payment.Transition transition) {
        switch (transition) {
            case CONFIRMED -> {
                publisher.confirmed(payment.getId(), new PaymentConfirmed(UUID.randomUUID(), Instant.now(),
                        payment.getId(), payment.getCustomerId(), payment.getMerchantId(), payment.getAmount()));
                log.info("Pagamento {} confirmado", payment.getId());
            }
            case REJECTED -> {
                publishRejected(payment);
                log.info("Pagamento {} rejeitado: {}", payment.getId(), payment.getReason());
            }
            case NONE -> {
                // aguardando o outro resultado
            }
        }
    }

    private void publishRejected(Payment payment) {
        publisher.rejected(payment.getId(), new PaymentRejected(UUID.randomUUID(), Instant.now(),
                payment.getId(), payment.getCustomerId(), payment.getMerchantId(), payment.getAmount(),
                payment.getReason()));
    }
}
