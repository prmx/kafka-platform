# Data Model

**Feature**: Real-Time Kafka Pricing Platform  
**Date**: 2025-10-03  
**Status**: Complete

## Overview
This document defines all domain entities, their relationships, validation rules, and state transitions for the Kafka pricing platform.

---

## Entity Catalog

### 1. PriceUpdate
**Module**: `kafka-platform-common`  
**Package**: `net.prmx.kafka.platform.common.model`  
**Type**: Immutable Value Object (Record)  
**Purpose**: Represents a single pricing event for a financial instrument

#### Fields

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| `instrumentId` | String | No | Instrument identifier in KEY format | Pattern: `KEY\d{6}`, Range: KEY000000-KEY999999 |
| `price` | double | No | Current market price | Must be > 0 |
| `timestamp` | long | No | Price generation time (epoch millis) | Must be > 0 |
| `bid` | double | No | Bid price (buy side) | Must be > 0, bid <= price |
| `ask` | double | No | Ask price (sell side) | Must be > 0, ask >= price |
| `volume` | int | No | Trade volume/quantity | Must be >= 0 |

#### Java Definition
```java
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
        if (price <= 0) throw new IllegalArgumentException("price must be positive");
        if (timestamp <= 0) throw new IllegalArgumentException("timestamp must be positive");
        if (bid <= 0) throw new IllegalArgumentException("bid must be positive");
        if (ask <= 0) throw new IllegalArgumentException("ask must be positive");
        if (volume < 0) throw new IllegalArgumentException("volume cannot be negative");
        if (bid > price) throw new IllegalArgumentException("bid cannot exceed price");
        if (ask < price) throw new IllegalArgumentException("ask cannot be less than price");
    }
}
```

#### Example JSON
```json
{
  "instrumentId": "KEY000123",
  "price": 105.75,
  "timestamp": 1696348800000,
  "bid": 105.70,
  "ask": 105.80,
  "volume": 1500
}
```

#### Relationships
- **Published by**: PriceGeneratorService (price-generator module)
- **Consumed by**: PriceUpdateConsumer (price-subscriber module)
- **Kafka Topic**: `price-updates` (keyed by instrumentId)

---

### 2. SubscriptionCommand
**Module**: `kafka-platform-common`  
**Package**: `net.prmx.kafka.platform.common.model`  
**Type**: Immutable Value Object (Record)  
**Purpose**: Represents a subscription configuration change command

#### Fields

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| `subscriberId` | String | No | Unique identifier for the subscriber instance | Not blank, max 100 chars |
| `action` | String | No | Command type: ADD, REMOVE, or REPLACE | Enum: [ADD, REMOVE, REPLACE] |
| `instrumentIds` | List<String> | No | List of instrument identifiers affected | Each must match KEY\d{6} pattern |
| `timestamp` | long | No | Command creation time (epoch millis) | Must be > 0 |

#### Java Definition
```java
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
        
        try {
            Action.valueOf(action);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "action must be one of: ADD, REMOVE, REPLACE. Got: " + action);
        }
        
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
```

#### Example JSON
```json
{
  "subscriberId": "subscriber-001",
  "action": "REPLACE",
  "instrumentIds": ["KEY000001", "KEY000050", "KEY001000"],
  "timestamp": 1696348800000
}
```

#### Relationships
- **Published by**: SubscriptionCommandService (subscription-manager module)
- **Consumed by**: SubscriptionCommandConsumer (price-subscriber module)
- **Kafka Topic**: `subscription-commands` (keyed by subscriberId, compacted)

---

### 3. PriceStatistics
**Module**: `kafka-platform-price-subscriber`  
**Package**: `net.prmx.kafka.platform.subscriber.model`  
**Type**: Mutable State Object  
**Purpose**: Aggregates statistics over a 5-second window for logging

#### Fields

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `totalMessages` | long | No | Total price updates received |
| `uniqueInstruments` | int | No | Number of unique instruments updated |
| `windowStartTime` | long | No | Start timestamp of current window (epoch millis) |
| `instrumentCounts` | Map<String, Long> | No | Per-instrument message counts |

#### Java Definition
```java
package net.prmx.kafka.platform.subscriber.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mutable state object tracking statistics for a 5-second window.
 * Used internally by StatisticsAggregator.
 */
public class PriceStatistics {
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final Map<String, AtomicLong> instrumentCounts = new ConcurrentHashMap<>();
    private final long windowStartTime;
    
    public PriceStatistics(long windowStartTime) {
        this.windowStartTime = windowStartTime;
    }
    
    public void recordMessage(String instrumentId) {
        totalMessages.incrementAndGet();
        instrumentCounts.computeIfAbsent(instrumentId, k -> new AtomicLong())
                       .incrementAndGet();
    }
    
    public long getTotalMessages() {
        return totalMessages.get();
    }
    
    public int getUniqueInstruments() {
        return instrumentCounts.size();
    }
    
    public long getWindowStartTime() {
        return windowStartTime;
    }
    
    public Map<String, Long> getInstrumentCounts() {
        Map<String, Long> snapshot = new ConcurrentHashMap<>();
        instrumentCounts.forEach((k, v) -> snapshot.put(k, v.get()));
        return snapshot;
    }
    
    public void reset() {
        totalMessages.set(0);
        instrumentCounts.clear();
    }
}
```

#### State Transitions
```
Created (windowStartTime set) 
  → Recording (recordMessage() calls accumulate)
  → Logged (getTotalMessages(), getUniqueInstruments() called)
  → Reset (reset() called, back to Recording)
```

#### Relationships
- **Created by**: StatisticsAggregator
- **Updated by**: PriceFilterService (on successful filter match)
- **Read by**: Scheduled logging task (every 5 seconds)

---

### 4. Instrument (Implicit Entity)
**Type**: Implicit Value (not a Java class)  
**Purpose**: Represents a tradable financial instrument

#### Properties
- **Identifier Format**: `KEYXXXXXX` where X is a digit (0-9)
- **Range**: KEY000000 to KEY999999 (50,000 total instruments)
- **Validation Regex**: `KEY\d{6}`

#### Validation Utility
**Module**: `kafka-platform-common`  
**Package**: `net.prmx.kafka.platform.common.util`

```java
package net.prmx.kafka.platform.common.util;

import java.util.regex.Pattern;

/**
 * Utility for validating instrument identifiers.
 */
public class InstrumentValidator {
    private static final Pattern INSTRUMENT_PATTERN = Pattern.compile("KEY\\d{6}");
    private static final String MIN_INSTRUMENT = "KEY000000";
    private static final String MAX_INSTRUMENT = "KEY999999";
    
    public static boolean isValid(String instrumentId) {
        if (instrumentId == null || !INSTRUMENT_PATTERN.matcher(instrumentId).matches()) {
            return false;
        }
        return instrumentId.compareTo(MIN_INSTRUMENT) >= 0 
            && instrumentId.compareTo(MAX_INSTRUMENT) <= 0;
    }
    
    public static void validateOrThrow(String instrumentId) {
        if (!isValid(instrumentId)) {
            throw new IllegalArgumentException(
                "Invalid instrument ID. Must be in range KEY000000-KEY999999: " + instrumentId);
        }
    }
    
    public static String formatInstrument(int index) {
        if (index < 0 || index > 999999) {
            throw new IllegalArgumentException("Index must be 0-999999: " + index);
        }
        return String.format("KEY%06d", index);
    }
}
```

---

## Entity Relationships Diagram

```
┌─────────────────────────┐
│  PriceGeneratorService  │
└───────────┬─────────────┘
            │ generates
            ▼
      ┌──────────────┐         Kafka Topic: price-updates
      │ PriceUpdate  │──────────────────────────────────────┐
      └──────────────┘                                       │
                                                             │ consumes
                                                             ▼
                                              ┌───────────────────────────┐
                                              │  PriceUpdateConsumer      │
                                              │  (price-subscriber)       │
                                              └────────────┬──────────────┘
                                                           │ filters using
                                                           ▼
                                              ┌───────────────────────────┐
                                              │  PriceFilterService       │
                                              │  (subscription config)    │
                                              └────────────┬──────────────┘
                                                           │ updates
                                                           ▼
                                              ┌───────────────────────────┐
                                              │  PriceStatistics          │
                                              └───────────────────────────┘

┌──────────────────────────┐
│  SubscriptionController  │
│  (REST API)              │
└───────────┬──────────────┘
            │ creates
            ▼
    ┌──────────────────────┐   Kafka Topic: subscription-commands
    │ SubscriptionCommand  │───────────────────────────────────────┐
    └──────────────────────┘   (compacted by subscriberId)         │
                                                                    │ consumes
                                                                    ▼
                                               ┌────────────────────────────────┐
                                               │ SubscriptionCommandConsumer    │
                                               │ (price-subscriber)             │
                                               └──────────────┬─────────────────┘
                                                              │ updates
                                                              ▼
                                               ┌────────────────────────────────┐
                                               │  PriceFilterService            │
                                               │  (subscription config)         │
                                               └────────────────────────────────┘
```

---

## Validation Rules Summary

### Cross-Entity Rules
1. **Price Consistency**: `bid <= price <= ask` for all PriceUpdate instances
2. **Instrument Universe**: All instrumentId values must be in KEY000000-KEY999999 range
3. **Timestamp Ordering**: Commands and updates should have monotonically increasing timestamps (not enforced, but expected)

### Module-Specific Rules
- **price-generator**: Must select random instruments from the valid 50,000 range
- **price-subscriber**: Must validate subscription lists before applying (reject invalid instruments)
- **subscription-manager**: Must validate REST requests and reject invalid instrument IDs with 400 Bad Request

---

## State Management

### PriceUpdate (Stateless)
- Immutable records
- No state transitions
- Created once, published, consumed, discarded

### SubscriptionCommand (Stateless with Compaction)
- Immutable records
- Kafka compaction manages state (latest command per subscriberId survives)
- Application state derived from consuming latest commands

### PriceStatistics (Stateful)
- Mutable state accumulates over 5-second windows
- State transitions: Created → Recording → Logged → Reset (cycle repeats)
- Thread-safe via atomic operations

### Subscription Configuration (Application State)
- Managed in-memory by PriceFilterService
- Updated by consuming SubscriptionCommand messages
- Actions:
  - **ADD**: `currentSet.addAll(instrumentIds)`
  - **REMOVE**: `currentSet.removeAll(instrumentIds)`
  - **REPLACE**: `currentSet = new HashSet<>(instrumentIds)`

---

## Schema Versioning

### Current Version: 1.0.0

**PriceUpdate Schema**:
- Version: 1.0.0
- Breaking change policy: Adding fields is backward-compatible, removing/renaming is MAJOR version bump

**SubscriptionCommand Schema**:
- Version: 1.0.0
- Breaking change policy: Action enum changes require coordination across services (MAJOR version bump)

**Future Considerations**:
- If Avro/Protobuf is adopted, schema registry would be required
- For now, JSON with version field (future enhancement) provides flexibility

---

**Status**: All entities defined and validated against feature requirements  
**Next**: Contract generation (Phase 1 continuation)
