package net.prmx.kafka.platform.subscriber.consumer;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import net.prmx.kafka.platform.subscriber.service.SubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for subscription commands from the subscription-commands topic.
 * Processes ADD/REMOVE/REPLACE commands to update subscription state.
 * 
 * Consumer Group: price-subscriber-group
 * Topic: subscription-commands (compacted)
 * 
 * Note: On startup, reads from beginning to rebuild subscription state from compacted log.
 */
@Component
public class SubscriptionCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCommandConsumer.class);

    private final SubscriptionManager subscriptionManager;

    public SubscriptionCommandConsumer(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    /**
     * Consume subscription command messages from Kafka.
     * Updates subscription state via SubscriptionManager.
     * 
     * @param command the subscription command
     */
    @KafkaListener(
        topics = "${kafka.topics.subscription-commands:subscription-commands}",
        groupId = "${kafka.consumer.group-id:price-subscriber-group}",
        containerFactory = "subscriptionCommandKafkaListenerContainerFactory"
    )
    public void consumeSubscriptionCommand(SubscriptionCommand command) {
        if (command == null) {
            log.warn("Received null subscription command, ignoring");
            return;
        }

        try {
            log.info("Received subscription command: action={}, subscriberId={}, instruments={}", 
                command.action(), command.subscriberId(), command.instrumentIds().size());
            
            // Apply command to update subscription state
            subscriptionManager.applyCommand(command);
            
        } catch (Exception e) {
            log.error("Error processing subscription command: {}", command, e);
            // Don't rethrow - continue processing other messages
        }
    }
}
