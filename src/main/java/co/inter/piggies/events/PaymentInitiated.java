package co.inter.piggies.events;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

/** contracts/events/payment-initiated.schema.json */
@Serdeable
public record PaymentInitiated(UUID eventId, Instant occurredAt, UUID paymentId,
                               String customerId, String merchantId, long amount) {
}
