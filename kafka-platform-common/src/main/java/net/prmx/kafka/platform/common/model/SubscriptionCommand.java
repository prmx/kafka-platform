package net.prmx.kafka.platform.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Immutable record representing a subscription change command.
 * Published to the subscription-commands Kafka topic (compacted).
 */
public record SubscriptionCommand(
    @JsonProperty("subscriberId") String subscriberId,
    @JsonProperty("action") String action,
    @JsonProperty("instrumentIds") List<String> instrumentIds,
    @JsonProperty("timestamp") long timestamp
) {
    public enum Action {
        ADD,      // Add instruments to existing subscription
        REMOVE,   // Remove instruments from existing subscription
        REPLACE   // Replace entire subscription list
    }

    @JsonCreator
    public SubscriptionCommand {
        Objects.requireNonNull(subscriberId, "subscriberId cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(instrumentIds, "instrumentIds cannot be null");

        if (subscriberId.isBlank()) {
            throw new IllegalArgumentException("subscriberId cannot be blank");
        }
        if (subscriberId.length() > 100) {
            throw new IllegalArgumentException("subscriberId max length is 100");
        }

        // Validate action is a valid enum value
        try {
            Action.valueOf(action);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "action must be one of: ADD, REMOVE, REPLACE. Got: " + action);
        }

        // Validate all instrument IDs
        for (String instrumentId : instrumentIds) {
            if (!instrumentId.matches("KEY\\d{6}")) {
                throw new IllegalArgumentException(
                    "Invalid instrumentId: " + instrumentId);
            }
        }

        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp must be positive");
        }

        // Make the list immutable
        instrumentIds = List.copyOf(instrumentIds);
    }
}
