package net.prmx.kafka.platform.subscriber.service;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import net.prmx.kafka.platform.subscriber.model.PriceStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Aggregates price update statistics over 5-second windows.
 * Uses @Scheduled to periodically log and reset counters.
 * 
 * Thread-safe implementation using PriceStatistics with atomic counters.
 */
@Service
public class StatisticsAggregator {

    private static final Logger log = LoggerFactory.getLogger(StatisticsAggregator.class);

    private final PriceStatistics statistics;

    public StatisticsAggregator() {
        this.statistics = new PriceStatistics();
    }

    /**
     * Record a price update message.
     * Thread-safe operation.
     * 
     * @param priceUpdate the price update to record
     */
    public void recordMessage(PriceUpdate priceUpdate) {
        statistics.recordMessage(priceUpdate);
    }

    /**
     * Log current statistics and reset counters for next window.
     * Triggered every 5 seconds by Spring's @Scheduled annotation.
     * 
     * Logs in structured JSON format:
     * - totalMessages: total count in this window
     * - uniqueInstruments: number of unique instruments
     * - top instruments: if needed for debugging
     */
    @Scheduled(fixedRate = 5000)
    public void logAndResetStatistics() {
        long total = statistics.getTotalMessages();
        int unique = statistics.getUniqueInstruments();

        // Log statistics (structured format for production monitoring)
        if (total > 0) {
            log.info("Statistics [5s window]: totalMessages={}, uniqueInstruments={}", 
                total, unique);
            
            // Optional: Log top instruments for debugging
            if (log.isDebugEnabled()) {
                statistics.getInstrumentCounts().forEach((instrumentId, count) -> 
                    log.debug("  {} -> {} messages", instrumentId, count.get())
                );
            }
        } else {
            log.info("Statistics [5s window]: totalMessages=0, uniqueInstruments=0");
        }

        // Reset for next window
        statistics.reset();
    }

    /**
     * Get current statistics (for testing).
     * 
     * @return current statistics object
     */
    PriceStatistics getStatistics() {
        return statistics;
    }
}
