package co.inter.piggies.reserve;

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
public class ReserveService {

    static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

    private final AccountRepository accounts;
    private final ReservationRepository reservations;
    private final EntityManager entityManager;
    private final ReserveEventPublisher publisher;

    @Transactional
    public Account deposit(String customerId, long amount) {
        Account account = lockAccount(customerId).orElseGet(() -> accounts.save(new Account(customerId)));
        account.deposit(amount);
        return accounts.update(account);
    }

    @Transactional
    public Optional<Account> find(String customerId) {
        return accounts.findById(customerId);
    }

    /** Reserva o valor do pagamento. Idempotente: evento repetido republica o mesmo resultado. */
    @Transactional
    public void reserve(PaymentInitiated event) {
        Reservation reservation = reservations.findById(event.paymentId())
                .orElseGet(() -> reservations.save(newReservation(event)));
        boolean ok = reservation.getStatus() != Reservation.Status.REJECTED;
        publisher.result(event.paymentId(), new ReserveResult(UUID.randomUUID(), Instant.now(), event.paymentId(),
                ok ? ReserveResult.Status.RESERVED : ReserveResult.Status.REJECTED, reservation.getReason()));
    }

    private Reservation newReservation(PaymentInitiated event) {
        Optional<Account> account = lockAccount(event.customerId());
        if (account.isEmpty()) {
            return Reservation.rejected(event.paymentId(), event.customerId(), event.amount(), ACCOUNT_NOT_FOUND);
        }
        if (!account.get().reserve(event.amount())) {
            return Reservation.rejected(event.paymentId(), event.customerId(), event.amount(), INSUFFICIENT_BALANCE);
        }
        log.info("Reservados {} Piggies de {} para pagamento {}", event.amount(), event.customerId(), event.paymentId());
        return Reservation.reserved(event.paymentId(), event.customerId(), event.amount());
    }

    /** Pagamento confirmado: efetiva o debito da reserva. */
    @Transactional
    public void capture(PaymentConfirmed event) {
        lockReservation(event.paymentId()).ifPresent(reservation -> {
            lockAccount(reservation.getCustomerId()).orElseThrow().capture(reservation.getAmount());
            reservation.capture();
            log.info("Debitados {} Piggies de {} (pagamento {})", reservation.getAmount(),
                    reservation.getCustomerId(), reservation.getPaymentId());
        });
    }

    /** Pagamento rejeitado: devolve o valor reservado ao saldo disponivel. */
    @Transactional
    public void release(PaymentRejected event) {
        lockReservation(event.paymentId()).ifPresent(reservation -> {
            lockAccount(reservation.getCustomerId()).orElseThrow().release(reservation.getAmount());
            reservation.release();
            log.info("Reserva do pagamento {} liberada", reservation.getPaymentId());
        });
    }

    /** Reserva ainda ativa (RESERVED); qualquer outro estado torna o evento um no-op. */
    private Optional<Reservation> lockReservation(UUID paymentId) {
        return Optional.ofNullable(entityManager.find(Reservation.class, paymentId, LockModeType.PESSIMISTIC_WRITE))
                .filter(r -> r.getStatus() == Reservation.Status.RESERVED);
    }

    private Optional<Account> lockAccount(String customerId) {
        return Optional.ofNullable(entityManager.find(Account.class, customerId, LockModeType.PESSIMISTIC_WRITE));
    }
}
