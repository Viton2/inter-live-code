package co.inter.piggies.merchant;

import co.inter.piggies.events.PaymentConfirmed;
import co.inter.piggies.events.PaymentInitiated;
import co.inter.piggies.events.Topics;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import lombok.RequiredArgsConstructor;

@KafkaListener(groupId = "piggies-merchant-service", offsetReset = OffsetReset.EARLIEST)
@RequiredArgsConstructor
public class MerchantListener {

    private final MerchantService service;

    @Topic(Topics.PAYMENT_INITIATED)
    void onInitiated(PaymentInitiated event) {
        service.validate(event);
    }

    @Topic(Topics.PAYMENT_CONFIRMED)
    void onConfirmed(PaymentConfirmed event) {
        service.credit(event);
    }
}
