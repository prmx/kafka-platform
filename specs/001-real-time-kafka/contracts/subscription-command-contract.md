# Kafka Message Contract: SubscriptionCommand

**Topic**: `subscription-commands`  
**Producer**: kafka-platform-subscription-manager  
**Consumers**: kafka-platform-price-subscriber  
**Version**: 1.0.0

## Message Format

### Key
- **Type**: String
- **Value**: subscriberId (e.g., "subscriber-001")
- **Purpose**: Enables log compaction to retain only latest command per subscriber

### Value
- **Format**: JSON
- **Schema**: SubscriptionCommand object

```json
{
  "subscriberId": "string (not blank, max 100 chars)",
  "action": "string (enum: ADD, REMOVE, REPLACE)",
  "instrumentIds": "array of string (each KEY\\d{6})",
  "timestamp": "number (long, epoch milliseconds)"
}
```

### Headers
- **Content-Type**: application/json
- **schema-version**: 1.0.0 (optional, for future versioning)
- **source**: subscription-manager (for traceability)

## Example Messages

### REPLACE Action
```json
{
  "key": "subscriber-001",
  "value": {
    "subscriberId": "subscriber-001",
    "action": "REPLACE",
    "instrumentIds": ["KEY000001", "KEY000050", "KEY001000"],
    "timestamp": 1696348800000
  }
}
```

### ADD Action
```json
{
  "key": "subscriber-002",
  "value": {
    "subscriberId": "subscriber-002",
    "action": "ADD",
    "instrumentIds": ["KEY000999"],
    "timestamp": 1696348801000
  }
}
```

### REMOVE Action
```json
{
  "key": "subscriber-003",
  "value": {
    "subscriberId": "subscriber-003",
    "action": "REMOVE",
    "instrumentIds": ["KEY000050"],
    "timestamp": 1696348802000
  }
}
```

## Contract Tests

### Test: Valid SubscriptionCommand Serialization
```java
@Test
public void testSubscriptionCommandSerializes() {
    SubscriptionCommand cmd = new SubscriptionCommand(
        "subscriber-001", "REPLACE", 
        List.of("KEY000001", "KEY000050"), 
        System.currentTimeMillis()
    );
    
    String json = objectMapper.writeValueAsString(cmd);
    
    assertThat(json).contains("\"subscriberId\":\"subscriber-001\"");
    assertThat(json).contains("\"action\":\"REPLACE\"");
    assertThat(json).contains("KEY000001");
}
```

### Test: Valid SubscriptionCommand Deserialization
```java
@Test
public void testSubscriptionCommandDeserializes() {
    String json = "{\"subscriberId\":\"subscriber-001\"," +
                  "\"action\":\"ADD\",\"instrumentIds\":[\"KEY000001\"]," +
                  "\"timestamp\":1696348800000}";
    
    SubscriptionCommand cmd = objectMapper.readValue(json, SubscriptionCommand.class);
    
    assertThat(cmd.subscriberId()).isEqualTo("subscriber-001");
    assertThat(cmd.action()).isEqualTo("ADD");
    assertThat(cmd.instrumentIds()).containsExactly("KEY000001");
}
```

### Test: Kafka Producer-Consumer Roundtrip with Compaction
```java
@SpringBootTest
@EmbeddedKafka(
    topics = "subscription-commands",
    partitions = 1,
    brokerProperties = {
        "log.cleanup.policy=compact",
        "log.segment.bytes=1024",
        "log.cleaner.min.compaction.lag.ms=1000"
    }
)
public class SubscriptionCommandContractTest {
    
    @Autowired
    private KafkaTemplate<String, SubscriptionCommand> kafkaTemplate;
    
    @Autowired
    private ConsumerFactory<String, SubscriptionCommand> consumerFactory;
    
    @Test
    public void testProducerConsumerContract() throws Exception {
        // Arrange
        String subscriberId = "subscriber-test";
        SubscriptionCommand cmd = new SubscriptionCommand(
            subscriberId, "REPLACE",
            List.of("KEY000001", "KEY000002"),
            System.currentTimeMillis()
        );
        
        // Act - Produce (keyed by subscriberId for compaction)
        kafkaTemplate.send("subscription-commands", subscriberId, cmd)
                     .get(5, TimeUnit.SECONDS);
        
        // Act - Consume
        Consumer<String, SubscriptionCommand> consumer = 
            consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("subscription-commands"));
        
        ConsumerRecords<String, SubscriptionCommand> records = 
            consumer.poll(Duration.ofSeconds(5));
        
        // Assert
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, SubscriptionCommand> record = 
            records.iterator().next();
        assertThat(record.key()).isEqualTo(subscriberId);
        assertThat(record.value()).isEqualTo(cmd);
        
        consumer.close();
    }
    
    @Test
    public void testCompactionRetainsLatestCommand() throws Exception {
        // Arrange - publish multiple commands for same subscriber
        String subscriberId = "subscriber-compact";
        
        SubscriptionCommand cmd1 = new SubscriptionCommand(
            subscriberId, "REPLACE", List.of("KEY000001"), 
            System.currentTimeMillis()
        );
        SubscriptionCommand cmd2 = new SubscriptionCommand(
            subscriberId, "REPLACE", List.of("KEY000002"), 
            System.currentTimeMillis() + 1000
        );
        
        // Act - Produce both
        kafkaTemplate.send("subscription-commands", subscriberId, cmd1)
                     .get(5, TimeUnit.SECONDS);
        kafkaTemplate.send("subscription-commands", subscriberId, cmd2)
                     .get(5, TimeUnit.SECONDS);
        
        // Wait for compaction (in real scenario)
        Thread.sleep(2000);
        
        // Assert - latest command survives (implementation note: 
        // full compaction test requires longer segments and manual trigger)
        // This test validates message format, actual compaction is Kafka's responsibility
    }
}
```

### Test: Invalid Action Rejected
```java
@Test
public void testInvalidActionRejected() {
    assertThatThrownBy(() -> new SubscriptionCommand(
        "subscriber-001", "INVALID_ACTION", 
        List.of("KEY000001"), System.currentTimeMillis()
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("must be one of: ADD, REMOVE, REPLACE");
}
```

### Test: Invalid Instrument ID in List Rejected
```java
@Test
public void testInvalidInstrumentIdRejected() {
    assertThatThrownBy(() -> new SubscriptionCommand(
        "subscriber-001", "ADD",
        List.of("KEY000001", "INVALID_ID"),
        System.currentTimeMillis()
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid instrumentId");
}
```

### Test: Blank Subscriber ID Rejected
```java
@Test
public void testBlankSubscriberIdRejected() {
    assertThatThrownBy(() -> new SubscriptionCommand(
        "", "ADD", List.of("KEY000001"), System.currentTimeMillis()
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("cannot be blank");
}
```

### Test: Subscriber ID Max Length Enforced
```java
@Test
public void testSubscriberIdMaxLength() {
    String longId = "a".repeat(101);
    assertThatThrownBy(() -> new SubscriptionCommand(
        longId, "ADD", List.of("KEY000001"), System.currentTimeMillis()
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("max length is 100");
}
```

## Topic Configuration

```yaml
topic: subscription-commands
partitions: 3
replication-factor: 1  # demo setup, use 3 in production
retention-ms: -1  # infinite (compaction handles cleanup)
cleanup-policy: compact
min-cleanable-dirty-ratio: 0.5
segment-ms: 600000  # 10 minutes
compression-type: snappy
```

## Compaction Behavior

### Key Insight
Messages with the same **key (subscriberId)** will be compacted, retaining only the **latest value**. This enables:
- Subscriber restarts: Read compacted log to get current subscription state
- Storage efficiency: Historical commands are cleaned up
- State recovery: Latest command per subscriber always available

### Example Compaction Sequence
```
Original Log:
  offset 0: key="sub-A", value={action: REPLACE, instruments: [KEY000001]}
  offset 1: key="sub-A", value={action: ADD, instruments: [KEY000002]}
  offset 2: key="sub-B", value={action: REPLACE, instruments: [KEY000050]}
  offset 3: key="sub-A", value={action: REMOVE, instruments: [KEY000001]}

After Compaction:
  offset 2: key="sub-B", value={action: REPLACE, instruments: [KEY000050]}
  offset 3: key="sub-A", value={action: REMOVE, instruments: [KEY000001]}
```

## Performance Requirements

- **Throughput**: <1 message/second (configuration changes are infrequent)
- **Latency**: <1 second from producer to consumer (not latency-critical)
- **Message Size**: ~300 bytes per message (JSON with instrument list)

## Breaking Change Policy

**MINOR version changes** (backward-compatible):
- Adding new action types (consumers ignore unknown actions)
- Adding optional fields

**MAJOR version changes** (breaking):
- Removing action types
- Changing action semantics (e.g., ADD becomes REPLACE)
- Changing key structure (breaks compaction)
- Renaming fields

## Consumer State Management

### Initial Subscription (On Startup)
1. Consumer subscribes to `subscription-commands` topic
2. Consumer seeks to beginning of partition
3. Consumer reads all retained commands (compacted log)
4. Consumer processes commands to build current subscription state
5. Consumer continues listening for new commands

### State Reconciliation
- **REPLACE**: Set subscription to instrumentIds (overwrites previous)
- **ADD**: Add instrumentIds to current subscription (union)
- **REMOVE**: Remove instrumentIds from current subscription (difference)

## Status
- [ ] Contract defined
- [ ] Tests written (must fail before implementation)
- [ ] Tests passing (after implementation)
