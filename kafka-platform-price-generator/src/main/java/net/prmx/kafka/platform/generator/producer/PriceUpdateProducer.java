package net.prmx.kafka.platform.generator.producer;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for publishing price updates.
 * Handles errors gracefully by logging and continuing (no retry, no blocking).
 */
@Component
public class PriceUpdateProducer {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdateProducer.class);

    private final KafkaTemplate<String, PriceUpdate> kafkaTemplate;
    private final String topic;

    public PriceUpdateProducer(
        KafkaTemplate<String, PriceUpdate> kafkaTemplate,
        @Value("${kafka.topics.price-updates:price-updates}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Publish a price update to Kafka.
     * Uses the instrumentId as the message key for partitioning.
     *
     * @param priceUpdate the price update to publish
     */
    public void publishPrice(PriceUpdate priceUpdate) {
        try {
            kafkaTemplate.send(topic, priceUpdate.instrumentId(), priceUpdate)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish price update for instrument {}: {}",
                            priceUpdate.instrumentId(), ex.getMessage());
                    } else {
                        log.trace("Successfully published price update for instrument {} to partition {}",
                            priceUpdate.instrumentId(), result.getRecordMetadata().partition());
                    }
                });
        } catch (Exception e) {
            log.error("Error sending price update for instrument {}: {}",
                priceUpdate.instrumentId(), e.getMessage());
        }
    }
}
