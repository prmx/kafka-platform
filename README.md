# Kafka Platform - Real-Time Price Distribution System

A high-performance, event-driven platform for distributing real-time price updates using Apache Kafka.

## Architecture

This is a multi-module Maven project with 4 modules:

- **kafka-platform-common**: Shared domain models (PriceUpdate, SubscriptionCommand, etc.)
- **kafka-platform-price-generator**: Publishes 1 million price updates per cycle (guaranteed coverage of all instruments)
- **kafka-platform-subscription-manager**: REST API for managing subscriber subscriptions
- **kafka-platform-price-subscriber**: Consumes and filters price updates based on subscriptions

## Technology Stack

- **Java 25** (with modern records and pattern matching)
- **Spring Boot 3.5.6** (with Spring for Apache Kafka)
- **Apache Kafka 4.0.0** (KRaft mode - no Zookeeper required)
- **Maven 3.9+** (multi-module project)
- **Docker** (for Kafka infrastructure)

## Prerequisites

- Java 25 JDK
- Maven 3.9+ (or use included Maven Wrapper)
- Docker (for running Kafka)

## Quick Start

### 1. Start Kafka Infrastructure

Start Apache Kafka 4.0.0 with KRaft mode (no Zookeeper needed):

```powershell
# Start Kafka container
docker-compose up -d

# Verify Kafka is running
docker logs kafka --tail 50

# Expected output: "Kafka Server started"
```

### 2. Create Kafka Topics

Create the required topics with specific configurations:

```powershell
# Create price-updates topic (10 partitions, delete cleanup policy, 1-hour retention)
docker exec kafka /opt/kafka/bin/kafka-topics.sh --create `
  --bootstrap-server localhost:9092 `
  --topic price-updates `
  --partitions 10 `
  --replication-factor 1 `
  --config cleanup.policy=delete `
  --config retention.ms=3600000

# Create subscription-commands topic (1 partition, compact cleanup policy)
docker exec kafka /opt/kafka/bin/kafka-topics.sh --create `
  --bootstrap-server localhost:9092 `
  --topic subscription-commands `
  --partitions 1 `
  --replication-factor 1 `
  --config cleanup.policy=compact

# Verify topics created
docker exec kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Expected output:
# price-updates
# subscription-commands
```

### 3. Build All Modules

Build the entire multi-module project:

```powershell
# Navigate to project root
cd d:\Develop\IdeaProjects\kafka-platform

# Build all modules (skip tests for faster build)
.\mvnw clean install -Dmaven.test.skip=true

# Expected output:
# [INFO] kafka-platform ...................................... SUCCESS
# [INFO] kafka-platform-common ............................... SUCCESS
# [INFO] kafka-platform-price-generator ...................... SUCCESS
# [INFO] kafka-platform-price-subscriber ..................... SUCCESS
# [INFO] kafka-platform-subscription-manager ................. SUCCESS
# [INFO] BUILD SUCCESS
```

### 4. Run the Services

#### Option A: Run in Background Jobs (Recommended for Testing)

Start all three services as PowerShell background jobs:

```powershell
# Start price-generator on port 8081
Start-Job -Name "price-generator" -ScriptBlock { 
  cd "D:\Develop\IdeaProjects\kafka-platform"
  java -jar kafka-platform-price-generator\target\kafka-platform-price-generator-0.0.1-SNAPSHOT.jar 
}

# Start subscription-manager on port 8080
Start-Job -Name "subscription-manager" -ScriptBlock { 
  cd "D:\Develop\IdeaProjects\kafka-platform"
  java -jar kafka-platform-subscription-manager\target\kafka-platform-subscription-manager-0.0.1-SNAPSHOT.jar 
}

# Start price-subscriber on port 8082
Start-Job -Name "price-subscriber" -ScriptBlock { 
  cd "D:\Develop\IdeaProjects\kafka-platform"
  java -jar kafka-platform-price-subscriber\target\kafka-platform-price-subscriber-0.0.1-SNAPSHOT.jar 
}

# Check job status
Get-Job

# View logs from a specific service
Receive-Job -Name "price-generator" | Select-Object -Last 20
Receive-Job -Name "subscription-manager" | Select-Object -Last 20
Receive-Job -Name "price-subscriber" | Select-Object -Last 20

# Stop all jobs when done
Stop-Job *; Remove-Job *
```

#### Option B: Run in Separate Terminal Windows

Start all three services in separate terminal windows:

**Terminal 1: Price Generator (Port 8081)**
```powershell
java -jar kafka-platform-price-generator\target\kafka-platform-price-generator-0.0.1-SNAPSHOT.jar
```

Expected output:
```
Started PriceGeneratorApplication in 8.5 seconds
Starting price generation in guaranteed coverage mode: 1000000 instruments per 60-second cycle
Completed cycle #1: published 1000000 price updates in 4 seconds (250000 msgs/sec)
```

**Terminal 2: Subscription Manager (Port 8080)**
```powershell
java -jar kafka-platform-subscription-manager\target\kafka-platform-subscription-manager-0.0.1-SNAPSHOT.jar
```

Expected output:
```
Started SubscriptionManagerApplication in 8.7 seconds
Tomcat started on port 8080 (http) with context path '/'
```

**Terminal 3: Price Subscriber (Port 8082)**
```powershell
java -jar kafka-platform-price-subscriber\target\kafka-platform-price-subscriber-0.0.1-SNAPSHOT.jar
```

Expected output:
```
Started PriceSubscriberApplication in 7.2 seconds
price-subscriber-group: partitions assigned: [price-updates-0, price-updates-1, ..., price-updates-9]
Statistics [5s window]: totalMessages=0, uniqueInstruments=0
```

### 5. Configure Subscriptions

Use the REST API to configure which instruments a subscriber should monitor.

**⚠️ Important**: The API endpoint is `/api/v1/subscriptions/` (note the `/v1` version prefix)

**Port Configuration:**
- Price Generator: 8081
- Subscription Manager: 8080 (REST API)
- Price Subscriber: 8082

#### Using PowerShell (Invoke-RestMethod):

```powershell
# REPLACE subscription - replaces entire instrument list
$body = @{ 
  instrumentIds = @("KEY000000", "KEY000001", "KEY000010", "KEY000100", "KEY001000", "KEY010000", "KEY100000", "KEY500000", "KEY999999") 
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/subscriptions/subscriber-001" `
  -Method PUT `
  -Body $body `
  -ContentType "application/json"
```

#### Using curl:

```powershell
# REPLACE subscription - replaces entire instrument list
curl -X PUT http://localhost:8080/api/v1/subscriptions/subscriber-001 `
  -H "Content-Type: application/json" `
  -d '{"instrumentIds": ["KEY000000", "KEY000001", "KEY000010"]}'
```

Expected response:
```json
{
  "subscriberId": "subscriber-001",
  "action": "REPLACE",
  "instrumentIds": ["KEY000000", "KEY000001", "KEY000010", "KEY000100", "KEY001000", "KEY010000", "KEY100000", "KEY500000", "KEY999999"],
  "timestamp": 1759526355743,
  "status": "PUBLISHED"
}
```

### 6. Observe Price Updates

Watch the Price Subscriber logs to see filtered price updates for subscribed instruments.

**Expected behavior:**
- The generator publishes 1 million instruments per cycle (every 60 seconds)
- Instruments are in shuffled random order: KEY000000 to KEY999999
- The subscriber filters and processes only subscribed instruments
- Statistics are logged every 5 seconds

**Example output:**

```
2025-10-03T23:19:16.263+02:00  INFO [price-subscriber] Received subscription command: action=REPLACE, subscriberId=subscriber-001, instruments=9
2025-10-03T23:19:16.263+02:00  INFO [price-subscriber] REPLACE subscription for subscriber-001: now subscribed to 9 instruments
2025-10-03T23:19:52.009+02:00  INFO [price-subscriber] Statistics [5s window]: totalMessages=1, uniqueInstruments=1
2025-10-03T23:19:52.009+02:00 DEBUG [price-subscriber]   KEY000001 -> 1 messages
2025-10-03T23:19:57.006+02:00  INFO [price-subscriber] Statistics [5s window]: totalMessages=3, uniqueInstruments=3
2025-10-03T23:19:57.006+02:00 DEBUG [price-subscriber]   KEY000000 -> 1 messages
2025-10-03T23:19:57.006+02:00 DEBUG [price-subscriber]   KEY000010 -> 1 messages
2025-10-03T23:19:57.006+02:00 DEBUG [price-subscriber]   KEY999999 -> 1 messages
2025-10-03T23:20:00.054+02:00 DEBUG [price-subscriber] Processed price update: KEY500000 @ 876.42357246995
2025-10-03T23:20:02.006+02:00  INFO [price-subscriber] Statistics [5s window]: totalMessages=4, uniqueInstruments=4
```

**If using background jobs:**

```powershell
# Continuously monitor subscriber statistics
while ($true) { 
  Clear-Host
  Receive-Job -Name "price-subscriber" | Select-String -Pattern "Statistics|Received subscription" | Select-Object -Last 10
  Start-Sleep -Seconds 5
}
```

## API Reference

### Subscription Manager REST API

**Base URL**: `http://localhost:8080/api/v1/subscriptions`

#### 1. Replace Subscription (PUT)
Replaces the entire subscription list for a subscriber.

**Endpoint**: `PUT /api/v1/subscriptions/{subscriberId}`

**Example (curl)**:
```powershell
curl -X PUT http://localhost:8080/api/v1/subscriptions/subscriber-001 `
  -H "Content-Type: application/json" `
  -d '{"instrumentIds": ["KEY000001", "KEY000002"]}'
```

**Example (PowerShell)**:
```powershell
$body = @{ instrumentIds = @("KEY000001", "KEY000002") } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/subscriptions/subscriber-001" `
  -Method PUT -Body $body -ContentType "application/json"
```

#### 2. Add Instruments (POST)
Adds instruments to an existing subscription.

**Endpoint**: `POST /api/v1/subscriptions/{subscriberId}/add`

**Example (curl)**:
```powershell
curl -X POST http://localhost:8080/api/v1/subscriptions/subscriber-001/add `
  -H "Content-Type: application/json" `
  -d '{"instrumentIds": ["KEY000003"]}'
```

**Example (PowerShell)**:
```powershell
$body = @{ instrumentIds = @("KEY000003") } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/subscriptions/subscriber-001/add" `
  -Method POST -Body $body -ContentType "application/json"
```

#### 3. Remove Instruments (POST)
Removes instruments from an existing subscription.

**Endpoint**: `POST /api/v1/subscriptions/{subscriberId}/remove`

**Example (curl)**:
```powershell
curl -X POST http://localhost:8080/api/v1/subscriptions/subscriber-001/remove `
  -H "Content-Type: application/json" `
  -d '{"instrumentIds": ["KEY000001"]}'
```

**Example (PowerShell)**:
```powershell
$body = @{ instrumentIds = @("KEY000001") } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/subscriptions/subscriber-001/remove" `
  -Method POST -Body $body -ContentType "application/json"
```

#### Response Format

All endpoints return the same response format:

```json
{
  "subscriberId": "subscriber-001",
  "action": "REPLACE",
  "instrumentIds": ["KEY000001", "KEY000002"],
  "timestamp": 1759526355743,
  "status": "PUBLISHED"
}
```

#### Error Responses

**Validation Error (400)**:
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed: instrumentIds - must not be empty; "
}
```

**Invalid Request (400)**:
```json
{
  "errorCode": "INVALID_REQUEST",
  "message": "instrumentIds cannot be null"
}
```

## Kafka Topic Details

### price-updates
- **Partitions**: 10 (for parallel processing)
- **Cleanup Policy**: delete
- **Retention**: 1 hour (3600000 ms)
- **Purpose**: High-volume price update stream

### subscription-commands
- **Partitions**: 1 (ensures ordering)
- **Cleanup Policy**: compact (keeps latest state per subscriber)
- **Purpose**: Subscription configuration changes

## Monitoring

### Health Checks
```powershell
# Price Generator
curl http://localhost:8081/actuator/health

# Subscription Manager
curl http://localhost:8080/actuator/health

# Price Subscriber
curl http://localhost:8082/actuator/health
```

### Kafka Consumer Groups
```powershell
# List all consumer groups
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
  --bootstrap-server localhost:9092 --list

# Describe price-subscriber group
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
  --bootstrap-server localhost:9092 `
  --group price-subscriber-group --describe
```

### Monitor Kafka Topics
```powershell
# Consume price-updates (see all generated prices)
docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server localhost:9092 `
  --topic price-updates `
  --from-beginning `
  --property print.key=true

# Consume subscription-commands (see configuration changes)
docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server localhost:9092 `
  --topic subscription-commands `
  --from-beginning `
  --property print.key=true
```

## Testing

### Run All Tests
```powershell
.\mvnw test
```

### Run Tests for Specific Module
```powershell
.\mvnw test -pl kafka-platform-price-subscriber
```

### Run Integration Tests Only
```powershell
.\mvnw test -Dtest=*IntegrationTest
```

## Troubleshooting

### Kafka Connection Refused
**Issue**: Services fail to connect to Kafka

**Solution**:
1. Verify Kafka is running: `docker ps`
2. Check Kafka logs: `docker logs kafka`
3. Ensure port 9092 is not blocked by firewall

### Topics Not Found
**Issue**: "Unknown topic or partition" errors

**Solution**:
1. Verify topics exist: `docker exec kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092`
2. Recreate topics using commands in section 2

### Subscriber Not Receiving Updates
**Issue**: Statistics always show 0 messages

**Solution**:
1. **Verify correct API endpoint**: Use `/api/v1/subscriptions/{subscriberId}` (not `/api/subscriptions`)
2. **Check subscription configured**: Look for "REPLACE subscription for subscriber-XXX" in subscriber logs
3. **Verify generator is publishing**: Check generator logs for "Completed cycle #N: published 1000000 price updates"
4. **Verify subscriber connected**: Check subscriber logs for "partitions assigned: [price-updates-0, ..., price-updates-9]"
5. **Wait for full cycle**: Generator publishes all 1M instruments every ~4-60 seconds (depending on throughput)
6. **Check instrument IDs**: Ensure subscribed instruments exist in range KEY000000 to KEY999999

**Debug commands**:
```powershell
# Check if generator is running and publishing
Receive-Job -Name "price-generator" | Select-String -Pattern "Completed cycle|published"

# Check if subscription command was received
Receive-Job -Name "price-subscriber" | Select-String -Pattern "Received subscription|REPLACE"

# Check current statistics
Receive-Job -Name "price-subscriber" | Select-String -Pattern "Statistics" | Select-Object -Last 5
```

## Cleanup

### Stop Services
```powershell
# Stop each Java service with Ctrl+C in their terminals
```

### Stop Kafka
```powershell
docker-compose down
```

### Delete Kafka Data Volume (complete reset)
```powershell
docker-compose down -v
```

## Project Structure

```
kafka-platform/
├── pom.xml                                    # Root parent POM
├── docker-compose.yml                         # Kafka infrastructure
├── README.md                                  # This file
├── kafka-platform-common/                     # Shared models
│   ├── pom.xml
│   └── src/main/java/.../common/
│       └── model/
│           ├── PriceUpdate.java
│           ├── SubscriptionCommand.java
│           └── PriceStatistics.java
├── kafka-platform-price-generator/            # Price publisher
│   ├── pom.xml
│   └── src/main/java/.../generator/
│       ├── PriceGeneratorApplication.java
│       └── service/
│           └── PriceGenerationService.java
├── kafka-platform-price-subscriber/           # Price consumer
│   ├── pom.xml
│   └── src/main/java/.../subscriber/
│       ├── PriceSubscriberApplication.java
│       ├── consumer/
│       │   ├── PriceUpdateConsumer.java
│       │   └── SubscriptionCommandConsumer.java
│       └── service/
│           ├── PriceFilterService.java
│           ├── StatisticsAggregator.java
│           └── SubscriptionManager.java
└── kafka-platform-subscription-manager/       # Subscription API
    ├── pom.xml
    └── src/main/java/.../manager/
        ├── SubscriptionManagerApplication.java
        ├── controller/
        │   └── SubscriptionController.java
        └── service/
            └── SubscriptionPublisher.java
```

## Performance Characteristics

### Actual Performance (Validated)

- **Throughput**: 250,000 messages/second (1M messages in 4 seconds)
- **Cycle Time**: 4-60 seconds per complete cycle of 1M instruments
- **Coverage**: 100% guaranteed - all 1M instruments published every cycle
- **Kafka Topics**: 10 partitions for parallel processing

### Performance Targets

- **Latency**: p95 < 500ms, p99 < 1s (generation to consumption)
- **Throughput**: 10,000+ price updates/second ✅ **Exceeded: 250,000/sec achieved**
- **Scalability**: Horizontal scaling via Kafka partitions

### Generator Behavior

- **Instrument Universe**: 1,000,000 instruments (KEY000000 to KEY999999)
- **Cycle-Based Publishing**: All instruments published every cycle in shuffled random order
- **Guaranteed Coverage**: Unlike random selection, ensures all instruments appear
- **Batch Processing**: 1,000 instruments per batch with 1ms delay
- **Logging**: Progress logged every 10,000 messages

## License

Proprietary - All Rights Reserved

## Support

For issues or questions, please contact the development team.
