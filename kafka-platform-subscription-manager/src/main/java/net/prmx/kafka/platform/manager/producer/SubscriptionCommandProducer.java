package net.prmx.kafka.platform.manager.producer;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * T044: Kafka producer for subscription commands
 * Uses subscriberId as message key for compaction
 */
@Component
public class SubscriptionCommandProducer {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCommandProducer.class);

    private final KafkaTemplate<String, SubscriptionCommand> kafkaTemplate;
    private final String topic;

    public SubscriptionCommandProducer(KafkaTemplate<String, SubscriptionCommand> kafkaTemplate,
                                       @Value("${kafka.topics.subscription-commands}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Publish subscription command with subscriberId as key
     */
    public void publishCommand(SubscriptionCommand command) {
        String key = command.subscriberId();
        
        CompletableFuture<SendResult<String, SubscriptionCommand>> future = 
                kafkaTemplate.send(topic, key, command);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published subscription command: subscriberId={}, action={}, instruments={}", 
                        command.subscriberId(), command.action(), command.instrumentIds().size());
            } else {
                log.error("Failed to publish subscription command: subscriberId={}, action={}", 
                        command.subscriberId(), command.action(), ex);
                // Retry logic could be added here with exponential backoff
                // For now, log error and let Spring Kafka's retry mechanism handle it
            }
        });
    }
}
