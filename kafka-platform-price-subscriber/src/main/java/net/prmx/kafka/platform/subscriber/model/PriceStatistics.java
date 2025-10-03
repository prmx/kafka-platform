package net.prmx.kafka.platform.subscriber.model;

import net.prmx.kafka.platform.common.model.PriceUpdate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mutable state object for 5-second window statistics aggregation.
 * Thread-safe implementation using atomic counters and concurrent collections.
 * 
 * Tracks:
 * - Total messages received in the current window
 * - Unique instruments seen in the current window
 * - Per-instrument message counts
 */
public class PriceStatistics {

    private final AtomicLong totalMessages;
    private final ConcurrentHashMap<String, AtomicLong> instrumentCounts;

    public PriceStatistics() {
        this.totalMessages = new AtomicLong(0);
        this.instrumentCounts = new ConcurrentHashMap<>();
    }

    /**
     * Record a price update message.
     * Increments total counter and per-instrument counter.
     * 
     * @param priceUpdate the price update to record
     */
    public void recordMessage(PriceUpdate priceUpdate) {
        if (priceUpdate == null) {
            return;
        }
        
        totalMessages.incrementAndGet();
        instrumentCounts
            .computeIfAbsent(priceUpdate.instrumentId(), k -> new AtomicLong(0))
            .incrementAndGet();
    }

    /**
     * Get total messages received in current window.
     * 
     * @return total message count
     */
    public long getTotalMessages() {
        return totalMessages.get();
    }

    /**
     * Get number of unique instruments seen in current window.
     * 
     * @return unique instrument count
     */
    public int getUniqueInstruments() {
        return instrumentCounts.size();
    }

    /**
     * Get detailed per-instrument counts.
     * 
     * @return concurrent map of instrument ID to message count
     */
    public ConcurrentHashMap<String, AtomicLong> getInstrumentCounts() {
        return instrumentCounts;
    }

    /**
     * Reset all counters to zero.
     * Called after logging statistics for the window.
     */
    public void reset() {
        totalMessages.set(0);
        instrumentCounts.clear();
    }
}
