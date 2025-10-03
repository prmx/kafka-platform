package net.prmx.kafka.platform.subscriber.consumer;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import net.prmx.kafka.platform.subscriber.service.PriceFilterService;
import net.prmx.kafka.platform.subscriber.service.StatisticsAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for price updates from the price-updates topic.
 * Filters incoming messages based on current subscriptions and records statistics.
 * 
 * Consumer Group: price-subscriber-group
 * Topic: price-updates
 */
@Component
public class PriceUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdateConsumer.class);

    private final PriceFilterService filterService;
    private final StatisticsAggregator statisticsAggregator;

    public PriceUpdateConsumer(PriceFilterService filterService, 
                               StatisticsAggregator statisticsAggregator) {
        this.filterService = filterService;
        this.statisticsAggregator = statisticsAggregator;
    }

    /**
     * Consume price update messages from Kafka.
     * Applies filtering and records statistics for subscribed instruments.
     * 
     * @param priceUpdate the price update message
     */
    @KafkaListener(
        topics = "${kafka.topics.price-updates:price-updates}",
        groupId = "${kafka.consumer.group-id:price-subscriber-group}",
        containerFactory = "priceUpdateKafkaListenerContainerFactory"
    )
    public void consumePriceUpdate(PriceUpdate priceUpdate) {
        if (priceUpdate == null) {
            log.warn("Received null price update, ignoring");
            return;
        }

        try {
            // Apply filter
            if (filterService.shouldProcess(priceUpdate)) {
                // Record statistics for subscribed instruments
                statisticsAggregator.recordMessage(priceUpdate);
                
                if (log.isDebugEnabled()) {
                    log.debug("Processed price update: {} @ {}", 
                        priceUpdate.instrumentId(), priceUpdate.price());
                }
            } else {
                // Instrument not subscribed, skip processing
                if (log.isTraceEnabled()) {
                    log.trace("Filtered out non-subscribed instrument: {}", 
                        priceUpdate.instrumentId());
                }
            }
        } catch (Exception e) {
            log.error("Error processing price update: {}", priceUpdate, e);
            // Don't rethrow - continue processing other messages
        }
    }
}
