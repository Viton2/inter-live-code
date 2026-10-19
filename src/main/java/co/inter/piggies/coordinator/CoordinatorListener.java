package co.inter.piggies.coordinator;

import co.inter.piggies.events.MerchantValidationResult;
import co.inter.piggies.events.ReserveResult;
import co.inter.piggies.events.Topics;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import lombok.RequiredArgsConstructor;

@KafkaListener(groupId = "piggies-payment-coordinator", offsetReset = OffsetReset.EARLIEST)
@RequiredArgsConstructor
public class CoordinatorListener {

    private final PaymentCoordinatorService service;

    @Topic(Topics.RESERVE_RESULT)
    void onReserveResult(ReserveResult result) {
        service.onReserveResult(result);
    }

    @Topic(Topics.MERCHANT_VALIDATION_RESULT)
    void onMerchantResult(MerchantValidationResult result) {
        service.onMerchantResult(result);
    }
}
