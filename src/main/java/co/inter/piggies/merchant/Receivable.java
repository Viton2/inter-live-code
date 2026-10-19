package co.inter.piggies.merchant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Credito de Piggies ao merchant por um pagamento confirmado. Um por paymentId (idempotencia). */
@Entity
@Table(name = "merchant_receivable")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receivable {

    @Id
    private UUID paymentId;

    @Column(nullable = false)
    private String merchantId;

    private long amount;

    @Column(nullable = false)
    private Instant createdAt;
}
