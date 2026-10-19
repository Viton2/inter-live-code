package co.inter.piggies.merchant;

import co.inter.piggies.events.MerchantValidationResult;
import co.inter.piggies.events.Topics;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

import java.util.UUID;

@KafkaClient(id = "piggies-merchant-service", acks = KafkaClient.Acknowledge.ALL)
public interface MerchantEventPublisher {

    @Topic(Topics.MERCHANT_VALIDATION_RESULT)
    void validationResult(@KafkaKey UUID paymentId, MerchantValidationResult event);
}
