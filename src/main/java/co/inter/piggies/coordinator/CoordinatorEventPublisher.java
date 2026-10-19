package co.inter.piggies.coordinator;

import co.inter.piggies.events.PaymentConfirmed;
import co.inter.piggies.events.PaymentInitiated;
import co.inter.piggies.events.PaymentRejected;
import co.inter.piggies.events.Topics;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

import java.util.UUID;

@KafkaClient(id = "piggies-payment-coordinator", acks = KafkaClient.Acknowledge.ALL)
public interface CoordinatorEventPublisher {

    @Topic(Topics.PAYMENT_INITIATED)
    void initiated(@KafkaKey UUID paymentId, PaymentInitiated event);

    @Topic(Topics.PAYMENT_CONFIRMED)
    void confirmed(@KafkaKey UUID paymentId, PaymentConfirmed event);

    @Topic(Topics.PAYMENT_REJECTED)
    void rejected(@KafkaKey UUID paymentId, PaymentRejected event);
}
