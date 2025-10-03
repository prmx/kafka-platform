# Research & Technology Decisions

**Feature**: Real-Time Kafka Pricing Platform  
**Date**: 2025-10-03  
**Status**: Complete

## Overview
This document captures technology decisions, best practices, and integration patterns for building a multi-module Spring Boot application with Kafka messaging for real-time price distribution.

---

## 1. Spring Boot & Kafka Integration

### Decision: Spring for Apache Kafka (spring-kafka)
**Version**: Compatible with Spring Boot 3.5.6

**Rationale**:
- Official Spring integration for Apache Kafka with auto-configuration
- Provides `KafkaTemplate` for simplified producer operations
- `@KafkaListener` annotation for declarative consumer configuration
- Seamless integration with Spring's dependency injection and configuration
- Built-in JSON serialization/deserialization support
- Testing support via `spring-kafka-test` with embedded Kafka

**Alternatives Considered**:
- **Native Kafka Java Client**: Rejected - requires more boilerplate, lacks Spring Boot auto-configuration
- **Kafka Streams API**: Not needed - simple pub/sub pattern sufficient for filtering use case

**Key Dependencies**:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 2. Message Serialization Strategy

### Decision: JSON Serialization with Jackson
**Format**: JSON over Kafka (String serializer with Jackson ObjectMapper)

**Rationale**:
- Human-readable for debugging and demonstration purposes
- Jackson is included with Spring Boot by default
- Simple schema evolution without external dependencies
- Suitable for demonstration platform scale (not production high-throughput)
- Easy integration with REST API in Subscription Manager

**Configuration**:
- Producer: `JsonSerializer` for values
- Consumer: `JsonDeserializer` with trusted packages configuration
- Message keys: String (instrument IDs or subscriber IDs)

**Alternatives Considered**:
- **Avro with Schema Registry**: Rejected - overkill for demonstration, adds infrastructure complexity
- **Protobuf**: Rejected - additional build tooling, not necessary for demo scale
- **Plain String**: Rejected - manual parsing error-prone

---

## 3. Kafka Topic Configuration

### Topic 1: price-updates
**Purpose**: Real-time price data distribution

**Configuration**:
- **Partitions**: 10 (allows parallel consumption, distributes load)
- **Replication Factor**: 1 (demonstration setup, would be 3 in production)
- **Retention**: `retention.ms=86400000` (24 hours)
- **Cleanup Policy**: `cleanup.policy=delete` (time-based)
- **Key**: instrumentId (String) - enables partitioning by instrument

**Rationale**:
- Multiple partitions enable horizontal scaling of consumers
- 24-hour retention balances storage with debugging capability
- Keyed by instrument ensures ordering per instrument

### Topic 2: subscription-commands
**Purpose**: Subscription configuration state management

**Configuration**:
- **Partitions**: 3 (lower throughput, configuration changes are infrequent)
- **Replication Factor**: 1 (demonstration setup)
- **Retention**: `retention.ms=-1` (infinite - compaction handles cleanup)
- **Cleanup Policy**: `cleanup.policy=compact` (retain latest state per key)
- **Key**: subscriberId (String) - enables compaction per subscriber

**Rationale**:
- Log compaction ensures latest configuration survives restarts
- Keyed by subscriber ID enables proper compaction per subscriber
- Infinite retention with compaction = state management pattern

---

## 4. Multi-Module Maven Structure

### Decision: 4-Module Layout (common + 3 services)

**Module Breakdown**:
1. **kafka-platform-common** (library)
   - Shared models: PriceUpdate, SubscriptionCommand
   - Utilities: InstrumentValidator
   - No Spring Boot application (plain JAR)
   
2. **kafka-platform-price-generator** (Spring Boot app)
   - Depends on: common
   - Role: Kafka producer
   
3. **kafka-platform-price-subscriber** (Spring Boot app)
   - Depends on: common
   - Role: Kafka consumer (dual: prices + commands)
   
4. **kafka-platform-subscription-manager** (Spring Boot app)
   - Depends on: common
   - Role: REST API + Kafka producer

**Rationale**:
- Aligns with constitutional requirement for multi-module structure
- Clear separation of concerns (one module per service + shared code)
- Enables independent deployment and scaling
- Shared models prevent duplication and ensure consistency
- Each module is independently testable

**Parent POM Responsibilities**:
- Dependency management (Spring Boot BOM)
- Plugin management (Maven Compiler, Spring Boot Maven Plugin)
- Java version enforcement (25)
- Module declarations

---

## 5. Testing Strategy

### Decision: 3-Tier Testing with Embedded Kafka

**Test Levels**:

1. **Unit Tests** (JUnit 5 + Mockito)
   - Mock Kafka producers/consumers
   - Test business logic in isolation
   - Target: 80%+ coverage
   - Example: PriceGenerationService logic, StatisticsAggregator

2. **Contract Tests** (Spring Kafka Test)
   - Verify message schemas (PriceUpdate, SubscriptionCommand)
   - Test serialization/deserialization
   - Validate message keys and headers
   - Run before implementation (TDD)

3. **Integration Tests** (Embedded Kafka)
   - Full producer-to-consumer flow
   - Test with real Kafka topics (in-memory)
   - Validate filtering, statistics, dynamic configuration
   - Use `@EmbeddedKafka` annotation

**Key Testing Dependencies**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Rationale**:
- Embedded Kafka eliminates external dependencies for CI/CD
- Contract tests ensure message compatibility across services
- Constitutional requirement for TDD and 80% coverage

---

## 6. Price Generation Algorithm

### Decision: Randomized Interval with Scheduled Executor

**Approach**:
```java
@Service
public class PriceGenerationService {
    private final ScheduledExecutorService scheduler = 
        Executors.newSingleThreadScheduledExecutor();
    
    public void startGeneration() {
        scheduleNextUpdate();
    }
    
    private void scheduleNextUpdate() {
        long delayMs = ThreadLocalRandom.current().nextLong(100, 1001);
        scheduler.schedule(() -> {
            generateAndPublishPrice();
            scheduleNextUpdate(); // recursive scheduling
        }, delayMs, TimeUnit.MILLISECONDS);
    }
}
```

**Rationale**:
- `ScheduledExecutorService` provides precise timing control
- Recursive scheduling allows dynamic interval per iteration
- ThreadLocalRandom for efficient randomization
- Single-threaded executor ensures sequential generation (no race conditions)

**Alternatives Considered**:
- **@Scheduled with fixedRate**: Rejected - doesn't support randomized intervals
- **Reactive Flux.interval**: Rejected - adds complexity without benefit for simple use case

---

## 7. Subscriber Filtering Strategy

### Decision: In-Application Filtering with Set-Based Lookup

**Approach**:
```java
@Component
public class PriceFilterService {
    private final Set<String> subscribedInstruments = 
        ConcurrentHashMap.newKeySet();
    
    public boolean shouldProcess(PriceUpdate update) {
        return subscribedInstruments.contains(update.getInstrumentId());
    }
    
    public void updateSubscriptions(Set<String> newInstruments) {
        subscribedInstruments.clear();
        subscribedInstruments.addAll(newInstruments);
    }
}
```

**Rationale**:
- O(1) lookup performance with HashSet
- ConcurrentHashMap.newKeySet() provides thread-safe Set
- Filters after consumption (not at Kafka level) - simple and flexible
- Easy to test and reason about

**Alternatives Considered**:
- **Kafka Streams filtering**: Rejected - overkill for simple in-memory filtering
- **Consumer assignment by pattern**: Rejected - doesn't support dynamic instrument lists
- **Database-backed filter**: Rejected - adds latency and complexity

---

## 8. Statistics Aggregation

### Decision: In-Memory Accumulator with Scheduled Flushing

**Approach**:
```java
@Service
public class StatisticsAggregator {
    private final AtomicLong messageCount = new AtomicLong(0);
    private final Map<String, AtomicLong> instrumentCounts = 
        new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 5000) // every 5 seconds
    public void logAndResetStatistics() {
        long count = messageCount.getAndSet(0);
        log.info("Statistics: totalMessages={}, instruments={}", 
            count, instrumentCounts.size());
        instrumentCounts.clear();
    }
    
    public void recordMessage(PriceUpdate update) {
        messageCount.incrementAndGet();
        instrumentCounts.computeIfAbsent(
            update.getInstrumentId(), k -> new AtomicLong()).incrementAndGet();
    }
}
```

**Rationale**:
- Atomic operations for thread-safe counting
- @Scheduled annotation for declarative 5-second intervals
- In-memory only (no persistence) - suitable for demonstration
- Minimal overhead (increments are O(1))

---

## 9. Subscription Manager REST API

### Decision: Simple REST Controller with Spring Web

**Endpoints**:
- `POST /subscriptions/{subscriberId}/add` - Add instruments
- `POST /subscriptions/{subscriberId}/remove` - Remove instruments
- `PUT /subscriptions/{subscriberId}` - Replace entire list
- `GET /subscriptions/{subscriberId}` - Get current subscription (optional)

**Request Serialization**:
- Synchronous processing (no async queue needed for demo scale)
- Service layer handles serialization of concurrent requests
- Uses `synchronized` or `ReentrantLock` per subscriber ID

**Rationale**:
- REST is simple and testable
- Spring Boot provides auto-configured web server (Tomcat)
- Synchronous processing acceptable for demonstration scale
- Constitutional requirement: REST API for management interface

**Alternatives Considered**:
- **gRPC**: Rejected - adds complexity, not necessary for demo
- **GraphQL**: Rejected - overkill for simple CRUD operations

---

## 10. Observability & Monitoring

### Decision: Spring Boot Actuator + Structured Logging

**Components**:
1. **Spring Boot Actuator**
   - Endpoint: `/actuator/health` (liveness/readiness)
   - Endpoint: `/actuator/metrics` (Kafka consumer lag, custom metrics)
   - Endpoint: `/actuator/prometheus` (optional - metrics export)

2. **Structured Logging (Logback + JSON)**
   - Format: JSON with fields (timestamp, level, message, service, traceId)
   - Encoder: `logstash-logback-encoder` for JSON formatting
   - Output: Console (containerized environments expect stdout)

3. **Custom Metrics**
   - Price generation rate (Micrometer gauge)
   - Price delivery latency (Micrometer timer)
   - Subscription change count (Micrometer counter)

**Configuration** (application.yml):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Rationale**:
- Constitutional requirement for observability
- Actuator is standard Spring Boot monitoring solution
- JSON logs enable easy parsing by log aggregators (ELK, Splunk)
- Micrometer provides unified metrics API

---

## 11. Configuration Management

### Decision: application.yml per Module with Profiles

**Structure**:
```yaml
spring:
  application:
    name: price-generator  # or price-subscriber, subscription-manager
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: ${spring.application.name}-group
      properties:
        spring.json.trusted.packages: net.prmx.kafka.platform.common.model

kafka:
  topics:
    price-updates: price-updates
    subscription-commands: subscription-commands

logging:
  level:
    net.prmx.kafka.platform: DEBUG
    org.springframework.kafka: INFO
```

**Rationale**:
- YAML is more readable than properties for nested structures
- Environment variable overrides (${VAR:default}) for Docker deployment
- Per-module configuration keeps concerns separated
- Trusted packages configuration prevents deserialization attacks

---

## 12. Error Handling Strategy

### Decision: Drop-and-Log for Generator, Retry for Consumers

**Price Generator**:
- On publish failure: Log error, drop message, continue
- No retry or buffering (maintains real-time characteristics)
- Aligns with specification requirement (FR-024)

**Price Subscriber**:
- On processing error: Log, optionally send to dead letter topic
- Consumer offset management: Manual acknowledgment after processing
- Retry with exponential backoff (Spring Kafka RetryTemplate)

**Subscription Manager**:
- REST API errors: Return proper HTTP status codes (400, 409, 500)
- Kafka publish errors: Retry with backoff, fail request if retries exhausted

**Rationale**:
- Aligns with specification edge case decisions
- Generator drop-and-continue ensures no blocking
- Consumer retries ensure message processing reliability
- Dead letter topics capture problematic messages for analysis

---

## Research Summary

All unknowns from Technical Context have been resolved:
- ✅ Spring Kafka selected for Kafka integration
- ✅ JSON serialization chosen for simplicity
- ✅ Dual-topic strategy defined (price-updates + subscription-commands)
- ✅ Multi-module Maven structure designed
- ✅ Testing strategy with embedded Kafka established
- ✅ Price generation algorithm defined (scheduled executor)
- ✅ Filtering strategy defined (in-memory Set)
- ✅ Statistics aggregation approach defined (atomic counters)
- ✅ REST API design completed
- ✅ Observability approach defined (Actuator + JSON logs)
- ✅ Configuration management strategy defined (YAML + profiles)
- ✅ Error handling strategy aligned with specifications

**No remaining NEEDS CLARIFICATION items.**

---

**Next Phase**: Phase 1 (Design & Contracts)
