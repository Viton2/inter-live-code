package co.inter.piggies.reserve;

import co.inter.piggies.events.PaymentConfirmed;
import co.inter.piggies.events.PaymentInitiated;
import co.inter.piggies.events.PaymentRejected;
import co.inter.piggies.events.Topics;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import lombok.RequiredArgsConstructor;

@KafkaListener(groupId = "piggies-reserve-service", offsetReset = OffsetReset.EARLIEST)
@RequiredArgsConstructor
public class ReserveListener {

    private final ReserveService service;

    @Topic(Topics.PAYMENT_INITIATED)
    void onInitiated(PaymentInitiated event) {
        service.reserve(event);
    }

    @Topic(Topics.PAYMENT_CONFIRMED)
    void onConfirmed(PaymentConfirmed event) {
        service.capture(event);
    }

    @Topic(Topics.PAYMENT_REJECTED)
    void onRejected(PaymentRejected event) {
        service.release(event);
    }
}
