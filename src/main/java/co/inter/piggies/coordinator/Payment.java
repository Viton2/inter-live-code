package co.inter.piggies.coordinator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Intencao de pagamento. Fica PENDING ate receber o resultado da reserva e da validacao
 * do merchant (que chegam em paralelo, em qualquer ordem); entao vira CONFIRMED ou REJECTED.
 */
@Entity
@Table(name = "coordinator_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    public enum Status { PENDING, CONFIRMED, REJECTED }

    public enum ReserveStatus { PENDING, RESERVED, REJECTED }

    public enum MerchantStatus { PENDING, VALID, INVALID }

    /** O que mudou no pagamento como efeito de um resultado recebido. */
    public enum Transition { NONE, CONFIRMED, REJECTED }

    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String merchantId;

    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReserveStatus reserveStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus merchantStatus;

    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    public static Payment initiate(String customerId, QrCode qrCode) {
        Payment p = new Payment();
        p.id = UUID.randomUUID();
        p.customerId = customerId;
        p.merchantId = qrCode.merchantId();
        p.amount = qrCode.amount();
        p.status = Status.PENDING;
        p.reserveStatus = ReserveStatus.PENDING;
        p.merchantStatus = MerchantStatus.PENDING;
        p.createdAt = Instant.now();
        return p;
    }

    public Transition onReserveResult(boolean reserved, String failureReason) {
        if (reserveStatus != ReserveStatus.PENDING) {
            return Transition.NONE; // evento duplicado
        }
        reserveStatus = reserved ? ReserveStatus.RESERVED : ReserveStatus.REJECTED;
        if (!reserved) {
            recordFailure(failureReason);
        }
        return evaluate();
    }

    public Transition onMerchantResult(boolean valid, String failureReason) {
        if (merchantStatus != MerchantStatus.PENDING) {
            return Transition.NONE; // evento duplicado
        }
        merchantStatus = valid ? MerchantStatus.VALID : MerchantStatus.INVALID;
        if (!valid) {
            recordFailure(failureReason);
        }
        return evaluate();
    }

    /** Reserva concluida depois que o pagamento ja foi rejeitado: precisa ser liberada. */
    public boolean hasOrphanReservation() {
        return status == Status.REJECTED && reserveStatus == ReserveStatus.RESERVED;
    }

    private void recordFailure(String failureReason) {
        if (reason == null) {
            reason = failureReason;
        }
    }

    private Transition evaluate() {
        if (status != Status.PENDING) {
            return Transition.NONE;
        }
        if (reserveStatus == ReserveStatus.REJECTED || merchantStatus == MerchantStatus.INVALID) {
            status = Status.REJECTED;
            return Transition.REJECTED;
        }
        if (reserveStatus == ReserveStatus.RESERVED && merchantStatus == MerchantStatus.VALID) {
            status = Status.CONFIRMED;
            return Transition.CONFIRMED;
        }
        return Transition.NONE;
    }
}
