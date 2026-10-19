package co.inter.piggies.events;

import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/** contracts/events/merchant-validation-result.schema.json */
@Serdeable
public record MerchantValidationResult(UUID eventId, Instant occurredAt, UUID paymentId,
                                       Status status, @Nullable String reason) {

    public enum Status { VALID, INVALID }
}
