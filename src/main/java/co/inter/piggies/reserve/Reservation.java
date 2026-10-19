package co.inter.piggies.reserve;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Reserva de saldo de um pagamento. Uma por paymentId (garante idempotencia). */
@Entity
@Table(name = "reserve_reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    public enum Status { RESERVED, REJECTED, CAPTURED, RELEASED }

    @Id
    private UUID paymentId;

    @Column(nullable = false)
    private String customerId;

    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String reason;

    private Reservation(UUID paymentId, String customerId, long amount, Status status, String reason) {
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
    }

    public static Reservation reserved(UUID paymentId, String customerId, long amount) {
        return new Reservation(paymentId, customerId, amount, Status.RESERVED, null);
    }

    public static Reservation rejected(UUID paymentId, String customerId, long amount, String reason) {
        return new Reservation(paymentId, customerId, amount, Status.REJECTED, reason);
    }

    public void capture() {
        status = Status.CAPTURED;
    }

    public void release() {
        status = Status.RELEASED;
    }
}
