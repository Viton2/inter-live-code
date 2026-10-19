package co.inter.piggies.reserve;

import co.inter.piggies.events.ReserveResult;
import co.inter.piggies.events.Topics;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

import java.util.UUID;

@KafkaClient(id = "piggies-reserve-service", acks = KafkaClient.Acknowledge.ALL)
public interface ReserveEventPublisher {

    @Topic(Topics.RESERVE_RESULT)
    void result(@KafkaKey UUID paymentId, ReserveResult event);
}
