package net.prmx.kafka.platform.subscriber.service;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Set-based filtering service for price updates.
 * Thread-safe implementation using ConcurrentHashMap.newKeySet().
 * 
 * Maintains the current subscription state and filters incoming price updates
 * based on whether the instrument is subscribed.
 */
@Component
public class PriceFilterService {

    private final Set<String> subscribedInstruments;

    public PriceFilterService() {
        this.subscribedInstruments = ConcurrentHashMap.newKeySet();
    }

    /**
     * Check if a price update should be processed based on current subscriptions.
     * 
     * @param priceUpdate the price update to check
     * @return true if the instrument is subscribed, false otherwise
     */
    public boolean shouldProcess(PriceUpdate priceUpdate) {
        if (priceUpdate == null) {
            return false;
        }
        return subscribedInstruments.contains(priceUpdate.instrumentId());
    }

    /**
     * Update the subscription set with new instruments.
     * Replaces the entire subscription set (not additive).
     * Thread-safe operation.
     * 
     * @param instruments new set of subscribed instrument IDs
     */
    public void updateSubscriptions(Set<String> instruments) {
        subscribedInstruments.clear();
        if (instruments != null) {
            subscribedInstruments.addAll(instruments);
        }
    }

    /**
     * Get current subscribed instruments (for testing/debugging).
     * 
     * @return unmodifiable view of subscribed instruments
     */
    public Set<String> getSubscribedInstruments() {
        return Set.copyOf(subscribedInstruments);
    }
}
