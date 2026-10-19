package co.inter.piggies.events;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

/** contracts/events/payment-confirmed.schema.json */
@Serdeable
public record PaymentConfirmed(UUID eventId, Instant occurredAt, UUID paymentId,
                               String customerId, String merchantId, long amount) {
}
