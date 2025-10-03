# Feature Specification: Real-Time Kafka Pricing Platform

**Feature Branch**: `001-real-time-kafka`  
**Created**: 2025-10-03  
**Status**: ✅ Ready for Planning  
**Input**: User description: "I am building a real-time Kafka pricing platform. The platform consists of three main services: 1. Prices Generator: This service must generate and publish price updates to a Kafka topic. It needs to simulate prices for 50,000 unique instruments with identifiers in the format KEYXXXXXX (from KEY000000 to KEY999999). A new price update for a random instrument should be published at a rate between 100 milliseconds and 1 second. 2. Prices Subscribers: This is a demonstration application that subscribes to price updates for a preselected list of instruments from the Kafka topic. Every 5 seconds, it must log statistics about the updates it has received, such as the total count of messages. 3. Prices Subscription Manager: This service controls the subscriptions for the Prices Subscribers. It should provide a mechanism (e.g., another Kafka topic or a simple API) to dynamically change the list of instruments the subscriber application is listening to."

---

## User Scenarios & Testing

### Primary User Story
As a financial data platform operator, I need a real-time pricing system that continuously generates price updates for 50,000 instruments, allows subscriber applications to selectively receive updates for specific instruments, and enables dynamic control over which instruments each subscriber monitors - all to demonstrate scalable, event-driven price distribution capabilities.

### Acceptance Scenarios

1. **Given** the Prices Generator service is running, **When** it generates price updates, **Then** a price update for a randomly selected instrument from the 50,000 instrument universe is published to the pricing topic at intervals between 100ms and 1 second.

2. **Given** the Prices Subscriber is configured to monitor a specific list of instruments (e.g., KEY000001, KEY000050, KEY001000), **When** price updates are published for those instruments, **Then** the subscriber receives only updates matching its subscription list.

3. **Given** the Prices Subscriber is running and receiving updates, **When** 5 seconds elapse, **Then** the subscriber logs statistical information including the total count of messages received during that period.

4. **Given** the Prices Subscription Manager receives a request to change the subscription list for a subscriber, **When** the new subscription is applied, **Then** the Prices Subscriber begins receiving updates only for the newly specified instruments without restart.

5. **Given** multiple Prices Subscriber instances are running with different subscription lists, **When** price updates are published, **Then** each subscriber independently receives only the updates relevant to its own subscription configuration.

6. **Given** a subscriber has received multiple subscription configuration changes over time, **When** a new subscriber instance starts or an existing instance restarts, **Then** it retrieves only the latest (compacted) subscription configuration from the control topic without processing historical changes.

### Edge Cases

- What happens when a price update is published for an instrument that no subscriber is monitoring? (Expected: Message published but not consumed, or consumed and ignored)
- How does the system handle when a subscriber's subscription list is empty? (Expected: Subscriber receives no price updates, continues logging zero-count statistics)
- What happens when the subscription list contains an invalid instrument identifier outside the KEY000000-KEY999999 range? (Expected: Invalid identifiers are validated and rejected at configuration time with a warning log entry. Only valid KEY000000-KEY999999 identifiers are accepted into the subscription list.)
- How does the system behave when the Prices Generator cannot publish to the topic due to connectivity issues? (Expected: Generator logs the error, drops the specific message, and continues attempting to publish subsequent updates. No buffering or blocking occurs to maintain real-time characteristics.)
- What happens when multiple subscription change requests are received simultaneously for the same subscriber? (Expected: Changes are processed sequentially in the order received. The Subscription Manager serializes concurrent requests to prevent race conditions.)

## Requirements

### Functional Requirements

#### Prices Generator Service
- **FR-001**: System MUST generate price updates for instruments with identifiers ranging from KEY000000 to KEY999999 (50,000 unique instruments).
- **FR-002**: System MUST select a random instrument from the 50,000 instrument universe for each price update generation cycle.
- **FR-003**: System MUST publish price updates at randomized intervals between 100 milliseconds and 1 second.
- **FR-004**: System MUST publish each generated price update to a designated pricing message topic.
- **FR-005**: Each price update MUST include the following attributes:
  - Instrument identifier (KEY format)
  - Price value (decimal number representing the current price)
  - Timestamp (ISO-8601 format indicating when the price was generated)
  - Bid price (decimal number for demonstration purposes)
  - Ask price (decimal number for demonstration purposes, typically slightly higher than bid)
  - Volume (integer representing the trade volume/quantity for demonstration purposes)

#### Prices Subscriber Service
- **FR-006**: System MUST allow configuration of a preselected list of instrument identifiers for subscription.
- **FR-007**: System MUST consume price updates from the pricing topic and filter to only process updates matching the subscribed instrument list.
- **FR-008**: System MUST aggregate statistics on received price updates over a 5-second rolling window.
- **FR-009**: System MUST log statistics (including total message count) every 5 seconds.
- **FR-010**: System MUST continue monitoring and logging even when no updates are received. Logs MUST show zero counts to demonstrate the subscriber is active and healthy.
- **FR-011**: System MUST support multiple concurrent subscriber instances with independent subscription configurations.

#### Prices Subscription Manager Service
- **FR-012**: System MUST provide a mechanism to receive subscription change requests for subscriber instances.
- **FR-013**: System MUST communicate subscription changes to the Prices Subscriber service via a dedicated Kafka control topic. This maintains the event-driven architecture and allows subscribers to react to configuration changes in real-time without polling.
- **FR-014**: System MUST support adding instruments to a subscriber's subscription list.
- **FR-015**: System MUST support removing instruments from a subscriber's subscription list.
- **FR-016**: System MUST support replacing the entire subscription list for a subscriber.
- **FR-017**: Subscription changes MUST take effect without requiring restart of the Prices Subscriber service.

### Performance & Scale Requirements
- **FR-018**: System MUST support the generation rate of 1-10 price updates per second (based on 100ms-1s interval).
- **FR-019**: System MUST handle subscribers monitoring anywhere from 1 to 50,000 instruments.
- **FR-020**: Subscriber statistics logging MUST complete within the 5-second window to avoid overlapping logs. Price update delivery latency SHOULD be under 500ms from generation to subscriber processing under normal operating conditions (target: p95 < 500ms, p99 < 1s).

### Data Retention & Lifecycle
- **FR-021**: Price updates topic MUST use time-based retention policy with a minimum of 24 hours retention. After 24 hours, older messages MAY be deleted to manage storage. For a demonstration platform, infinite retention is not required.
- **FR-025**: Subscription control topic MUST use log compaction (cleanup.policy=compact) to retain only the latest subscription configuration for each subscriber. This ensures subscribers can always recover their current configuration state without processing historical changes.
- **FR-026**: Compacted control topic messages MUST use the subscriber identifier as the message key to enable proper compaction per subscriber instance.

### Key Entities

- **Price Update**: Represents a single pricing event for a financial instrument. Contains the instrument identifier (KEY format), price information (price, bid, ask), timestamp, and volume. Published by the Prices Generator and consumed by Prices Subscribers.

- **Instrument**: Represents a tradable financial entity identified by a unique key in the format KEYXXXXXX where XXXXXX is a zero-padded number from 000000 to 999999. The platform supports exactly 50,000 instruments.

- **Subscription Configuration**: Represents the list of instrument identifiers that a specific subscriber instance is monitoring. Can be dynamically updated by the Subscription Manager without service restart.

- **Price Statistics**: Represents aggregated metrics collected by a subscriber over a 5-second window. Includes at minimum the total count of received price updates, and potentially other metrics like instruments updated, average update rate.

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

**Clarifications Resolved**:
1. ✅ Price update attributes: instrument ID, price, timestamp, bid, ask, volume
2. ✅ Invalid instrument handling: validated and rejected at configuration time
3. ✅ Generator publish failures: log error, drop message, continue (no buffering)
4. ✅ Concurrent subscription changes: serialized processing in order received
5. ✅ Zero-update logging: always log including zero counts for health visibility
6. ✅ Subscription Manager mechanism: dedicated Kafka control topic (event-driven)
7. ✅ Retention & latency: 24-hour retention for prices, p95 < 500ms delivery latency
8. ✅ Topic compaction: Control topic uses log compaction to retain latest subscription state per subscriber

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked and resolved (7 clarifications completed)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---
