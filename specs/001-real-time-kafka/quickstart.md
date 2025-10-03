# Quickstart Guide: Real-Time Kafka Pricing Platform

**Version**: 1.0.0  
**Date**: 2025-10-03  
**Target Audience**: Developers, QA Engineers, Platform Operators

## Overview
This quickstart guide provides step-by-step instructions to build, test, and run the Kafka pricing platform locally. By the end of this guide, you will have all three services running, generating and consuming price updates, and dynamically managing subscriptions.

---

## Prerequisites

### Required Software
- **Java 25** (JDK 25 installed and `JAVA_HOME` set)
- **Maven 3.9+** (or use included Maven Wrapper `mvnw`)
- **Apache Kafka** (local installation or Docker)
- **Git** (for cloning the repository)

### Verify Installation
```powershell
# Verify Java version
java -version
# Should output: java version "25..."

# Verify Maven
mvn -version
# Should output: Apache Maven 3.9.x or higher

# Verify Kafka (if using local installation)
kafka-topics.bat --version
# Should output Kafka version information
```

---

## Step 1: Start Apache Kafka

### Option A: Using Docker (Recommended for Demo)
```powershell
# Start Kafka and Zookeeper using Docker Compose
# Create docker-compose.yml in project root:

version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1

# Start containers
docker-compose up -d

# Verify Kafka is running
docker ps
# Should show zookeeper and kafka containers
```

### Option B: Using Local Kafka Installation
```powershell
# Start Zookeeper
cd $KAFKA_HOME
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties

# In a new terminal, start Kafka
cd $KAFKA_HOME
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

---

## Step 2: Create Kafka Topics

```powershell
# Create price-updates topic (time-based retention)
kafka-topics.bat --create `
  --bootstrap-server localhost:9092 `
  --topic price-updates `
  --partitions 10 `
  --replication-factor 1 `
  --config retention.ms=86400000 `
  --config cleanup.policy=delete

# Create subscription-commands topic (compacted)
kafka-topics.bat --create `
  --bootstrap-server localhost:9092 `
  --topic subscription-commands `
  --partitions 3 `
  --replication-factor 1 `
  --config cleanup.policy=compact `
  --config retention.ms=-1 `
  --config segment.ms=600000

# Verify topics created
kafka-topics.bat --list --bootstrap-server localhost:9092
# Should output:
# price-updates
# subscription-commands
```

---

## Step 3: Build the Multi-Module Project

```powershell
# Navigate to project root
cd d:\Develop\IdeaProjects\kafka-platform

# Build all modules (using Maven Wrapper)
.\mvnw clean install

# Expected output:
# [INFO] Reactor Summary:
# [INFO] kafka-platform ..................................... SUCCESS
# [INFO] kafka-platform-common .............................. SUCCESS
# [INFO] kafka-platform-price-generator ..................... SUCCESS
# [INFO] kafka-platform-price-subscriber .................... SUCCESS
# [INFO] kafka-platform-subscription-manager ................ SUCCESS
# [INFO] BUILD SUCCESS
```

**Note**: All tests should pass. If contract tests fail, it indicates missing implementations (expected in TDD workflow).

---

## Step 4: Run the Services

### Terminal 1: Start Price Generator
```powershell
cd kafka-platform-price-generator
..\mvnw spring-boot:run

# Expected output:
# Started PriceGeneratorApplication in X seconds
# Generating price updates every 100-1000ms
# Published PriceUpdate for KEY000123 at 1696348800000
```

### Terminal 2: Start Price Subscriber
```powershell
cd kafka-platform-price-subscriber
..\mvnw spring-boot:run

# Expected output:
# Started PriceSubscriberApplication in X seconds
# Subscribed to instruments: []
# [Every 5 seconds] Statistics: totalMessages=0, instruments=0
```

### Terminal 3: Start Subscription Manager
```powershell
cd kafka-platform-subscription-manager
..\mvnw spring-boot:run

# Expected output:
# Started SubscriptionManagerApplication in X seconds
# Tomcat started on port 8080
```

---

## Step 5: Configure Subscriptions via REST API

```powershell
# Set subscription for subscriber-001 to monitor 3 instruments
curl -X PUT http://localhost:8080/api/v1/subscriptions/subscriber-001 `
  -H "Content-Type: application/json" `
  -d '{
    "instrumentIds": ["KEY000001", "KEY000050", "KEY001000"]
  }'

# Expected response:
# {
#   "subscriberId": "subscriber-001",
#   "action": "REPLACE",
#   "instrumentIds": ["KEY000001", "KEY000050", "KEY001000"],
#   "timestamp": 1696348800000,
#   "status": "PUBLISHED"
# }
```

**Result**: Price Subscriber will receive the command via Kafka and update its filter. It will now log statistics for the 3 subscribed instruments.

---

## Step 6: Observe Price Updates

### Monitor Price Subscriber Logs
Watch Terminal 2 (Price Subscriber) for statistics logs every 5 seconds:

```
2025-10-03T10:00:05.000Z INFO  - Statistics: totalMessages=12, uniqueInstruments=3
2025-10-03T10:00:10.000Z INFO  - Statistics: totalMessages=8, uniqueInstruments=2
2025-10-03T10:00:15.000Z INFO  - Statistics: totalMessages=15, uniqueInstruments=3
```

**Explanation**:
- `totalMessages`: Number of price updates received in the last 5 seconds
- `uniqueInstruments`: Number of distinct instruments updated

### Monitor Kafka Topics (Optional)
```powershell
# Consume price-updates topic (see all generated prices)
kafka-console-consumer.bat `
  --bootstrap-server localhost:9092 `
  --topic price-updates `
  --from-beginning `
  --property print.key=true `
  --property key.separator=": "

# Consume subscription-commands topic (see configuration changes)
kafka-console-consumer.bat `
  --bootstrap-server localhost:9092 `
  --topic subscription-commands `
  --from-beginning `
  --property print.key=true `
  --property key.separator=": "
```

---

## Step 7: Test Dynamic Subscription Changes

### Add Instruments
```powershell
curl -X POST http://localhost:8080/api/v1/subscriptions/subscriber-001/add `
  -H "Content-Type: application/json" `
  -d '{
    "instrumentIds": ["KEY000999"]
  }'
```

**Result**: Subscriber now monitors 4 instruments (3 original + 1 added). Statistics will reflect updates from KEY000999.

### Remove Instruments
```powershell
curl -X POST http://localhost:8080/api/v1/subscriptions/subscriber-001/remove `
  -H "Content-Type: application/json" `
  -d '{
    "instrumentIds": ["KEY000050"]
  }'
```

**Result**: Subscriber now monitors 3 instruments (removed KEY000050). No more statistics for that instrument.

### Replace Entire Subscription
```powershell
curl -X PUT http://localhost:8080/api/v1/subscriptions/subscriber-001 `
  -H "Content-Type: application/json" `
  -d '{
    "instrumentIds": ["KEY000100", "KEY000200"]
  }'
```

**Result**: Subscriber now monitors only 2 instruments (previous list replaced).

---

## Step 8: Test Edge Cases

### Empty Subscription List
```powershell
curl -X PUT http://localhost:8080/api/v1/subscriptions/subscriber-001 `
  -H "Content-Type: application/json" `
  -d '{
    "instrumentIds": []
  }'
```

**Expected**: 400 Bad Request (validation error: instrumentIds cannot be empty)

### Invalid Instrument ID
```powershell
curl -X PUT http://localhost:8080/api/v1/subscriptions/subscriber-001 `
  -H "Content-Type: application/json" `
  -d '{
    "instrumentIds": ["INVALID_ID"]
  }'
```

**Expected**: 400 Bad Request (validation error: Invalid instrument ID format)

### Subscriber Restart (State Recovery)
```powershell
# Stop the subscriber (Ctrl+C in Terminal 2)
# Restart the subscriber
cd kafka-platform-price-subscriber
..\mvnw spring-boot:run
```

**Expected**: Subscriber reads compacted `subscription-commands` topic and restores the latest subscription configuration without manual reconfiguration.

---

## Step 9: Monitor Health and Metrics

### Health Check Endpoints
```powershell
# Price Generator health
curl http://localhost:8081/actuator/health

# Price Subscriber health
curl http://localhost:8082/actuator/health

# Subscription Manager health
curl http://localhost:8080/actuator/health
```

**Expected**: All services return `{"status": "UP"}`

### Prometheus Metrics
```powershell
# View metrics for Subscription Manager
curl http://localhost:8080/actuator/prometheus

# Look for custom metrics:
# - kafka_producer_record_send_total
# - subscription_command_published_total
# - price_update_latency_seconds
```

---

## Step 10: Run Integration Tests

```powershell
# Run tests for all modules
cd d:\Develop\IdeaProjects\kafka-platform
.\mvnw test

# Run tests for a specific module
cd kafka-platform-price-subscriber
..\mvnw test

# Run only integration tests
.\mvnw test -Dtest=*IntegrationTest
```

**Expected**: All tests pass. Integration tests use embedded Kafka (no external Kafka required).

---

## Troubleshooting

### Issue: Kafka Connection Refused
**Symptom**: Services fail to start with "Connection refused: localhost/127.0.0.1:9092"

**Solution**:
1. Verify Kafka is running: `docker ps` or check local Kafka process
2. Verify bootstrap server configuration in `application.yml` matches Kafka address
3. Check firewall settings

### Issue: Topics Not Found
**Symptom**: "Unknown topic or partition" errors

**Solution**:
1. Recreate topics using commands in Step 2
2. Verify topic names match configuration: `price-updates`, `subscription-commands`

### Issue: Price Subscriber Not Receiving Updates
**Symptom**: Statistics always show 0 messages

**Solution**:
1. Verify subscription configured via REST API (Step 5)
2. Check that generator is publishing (view generator logs)
3. Verify consumer group ID is unique (avoid conflicts)

### Issue: Build Fails with Java Version Error
**Symptom**: "java.lang.UnsupportedClassVersionError" or "source release 25 requires target release 25"

**Solution**:
1. Verify Java 25 installed: `java -version`
2. Set `JAVA_HOME` environment variable to Java 25 JDK path
3. Rebuild: `.\mvnw clean install`

---

## Cleanup

### Stop Services
```powershell
# Stop each service with Ctrl+C in their terminals
```

### Stop Kafka (Docker)
```powershell
docker-compose down
```

### Stop Kafka (Local)
```powershell
# Stop Kafka broker (Ctrl+C)
# Stop Zookeeper (Ctrl+C)
```

### Delete Topics (Optional)
```powershell
kafka-topics.bat --delete --bootstrap-server localhost:9092 --topic price-updates
kafka-topics.bat --delete --bootstrap-server localhost:9092 --topic subscription-commands
```

---

## Next Steps

- **Load Testing**: Generate price updates at higher rates (reduce interval to 10ms)
- **Multiple Subscribers**: Start multiple subscriber instances with different IDs
- **Performance Monitoring**: Analyze metrics in Prometheus/Grafana
- **Schema Evolution**: Add new fields to PriceUpdate and test backward compatibility
- **Production Deployment**: Containerize services with Docker and deploy to Kubernetes

---

## Acceptance Criteria Validation

### ✅ Scenario 1: Price Generator Publishes Updates
- **Expected**: Generator logs show "Published PriceUpdate for KEY..." every 100ms-1s
- **Validation**: Check generator logs or consume `price-updates` topic

### ✅ Scenario 2: Subscriber Receives Filtered Updates
- **Expected**: Subscriber only logs statistics for subscribed instruments
- **Validation**: Compare generator output to subscriber statistics

### ✅ Scenario 3: Statistics Logged Every 5 Seconds
- **Expected**: Subscriber logs "Statistics: totalMessages=X, uniqueInstruments=Y" every 5 seconds
- **Validation**: Observe subscriber logs with timestamp

### ✅ Scenario 4: Dynamic Subscription Changes
- **Expected**: Subscriber immediately reflects subscription changes without restart
- **Validation**: Change subscription via API, observe updated statistics

### ✅ Scenario 5: Multiple Independent Subscribers
- **Expected**: Start 2+ subscribers with different IDs, each maintains independent subscriptions
- **Validation**: Configure different subscriptions, verify independent statistics

### ✅ Scenario 6: State Recovery After Restart
- **Expected**: Restarted subscriber recovers latest subscription from compacted topic
- **Validation**: Configure subscription, restart subscriber, verify configuration restored

---

**Status**: Quickstart guide complete and ready for validation during implementation.
