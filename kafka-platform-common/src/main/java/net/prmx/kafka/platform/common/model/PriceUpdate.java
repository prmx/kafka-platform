package net.prmx.kafka.platform.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Immutable record representing a price update for a financial instrument.
 * Published to the price-updates Kafka topic.
 */
public record PriceUpdate(
    @JsonProperty("instrumentId") String instrumentId,
    @JsonProperty("price") double price,
    @JsonProperty("timestamp") long timestamp,
    @JsonProperty("bid") double bid,
    @JsonProperty("ask") double ask,
    @JsonProperty("volume") int volume
) {
    @JsonCreator
    public PriceUpdate {
        Objects.requireNonNull(instrumentId, "instrumentId cannot be null");
        if (!instrumentId.matches("KEY\\d{6}")) {
            throw new IllegalArgumentException(
                "instrumentId must match pattern KEY\\d{6}: " + instrumentId);
        }
        if (price <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp must be positive");
        }
        if (bid <= 0) {
            throw new IllegalArgumentException("bid must be positive");
        }
        if (ask <= 0) {
            throw new IllegalArgumentException("ask must be positive");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("volume cannot be negative");
        }
        if (bid > price) {
            throw new IllegalArgumentException("bid cannot exceed price");
        }
        if (ask < price) {
            throw new IllegalArgumentException("ask cannot be less than price");
        }
    }
}
