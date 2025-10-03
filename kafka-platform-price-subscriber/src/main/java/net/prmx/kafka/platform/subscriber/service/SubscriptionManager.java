package net.prmx.kafka.platform.subscriber.service;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages subscription state with ADD/REMOVE/REPLACE logic.
 * Processes SubscriptionCommand messages and updates the PriceFilterService.
 * 
 * State machine:
 * - ADD: Adds instruments to existing subscription set
 * - REMOVE: Removes instruments from existing subscription set
 * - REPLACE: Replaces entire subscription set with new instruments
 */
@Service
public class SubscriptionManager {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class);

    private final PriceFilterService filterService;
    private final Set<String> currentSubscriptions;

    public SubscriptionManager(PriceFilterService filterService) {
        this.filterService = filterService;
        this.currentSubscriptions = new HashSet<>();
    }

    /**
     * Apply a subscription command and update the filter service.
     * Thread-safe operation (synchronized on internal state).
     * 
     * @param command the subscription command to apply
     */
    public synchronized void applyCommand(SubscriptionCommand command) {
        if (command == null) {
            log.warn("Received null subscription command, ignoring");
            return;
        }

        String action = command.action();
        if (action == null) {
            log.warn("Received subscription command with null action, ignoring: {}", command);
            return;
        }

        try {
            switch (action) {
                case "ADD" -> handleAdd(command);
                case "REMOVE" -> handleRemove(command);
                case "REPLACE" -> handleReplace(command);
                default -> log.warn("Unknown subscription action: {}, ignoring command", action);
            }
        } catch (Exception e) {
            log.error("Error applying subscription command: {}", command, e);
        }
    }

    private void handleAdd(SubscriptionCommand command) {
        Set<String> instrumentsToAdd = new HashSet<>(command.instrumentIds());
        currentSubscriptions.addAll(instrumentsToAdd);
        
        filterService.updateSubscriptions(new HashSet<>(currentSubscriptions));
        
        log.info("ADD subscription for {}: added {} instruments, total now: {}", 
            command.subscriberId(), instrumentsToAdd.size(), currentSubscriptions.size());
    }

    private void handleRemove(SubscriptionCommand command) {
        Set<String> instrumentsToRemove = new HashSet<>(command.instrumentIds());
        currentSubscriptions.removeAll(instrumentsToRemove);
        
        filterService.updateSubscriptions(new HashSet<>(currentSubscriptions));
        
        log.info("REMOVE subscription for {}: removed {} instruments, total now: {}", 
            command.subscriberId(), instrumentsToRemove.size(), currentSubscriptions.size());
    }

    private void handleReplace(SubscriptionCommand command) {
        Set<String> newSubscriptions = new HashSet<>(command.instrumentIds());
        currentSubscriptions.clear();
        currentSubscriptions.addAll(newSubscriptions);
        
        filterService.updateSubscriptions(new HashSet<>(currentSubscriptions));
        
        log.info("REPLACE subscription for {}: now subscribed to {} instruments", 
            command.subscriberId(), currentSubscriptions.size());
    }

    /**
     * Get current subscriptions (for testing/debugging).
     * 
     * @return copy of current subscriptions
     */
    public synchronized Set<String> getCurrentSubscriptions() {
        return new HashSet<>(currentSubscriptions);
    }
}
