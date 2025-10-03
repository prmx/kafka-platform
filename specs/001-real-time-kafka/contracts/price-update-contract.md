# Kafka Message Contract: PriceUpdate

**Topic**: `price-updates`  
**Producer**: kafka-platform-price-generator  
**Consumers**: kafka-platform-price-subscriber  
**Version**: 1.0.0

## Message Format

### Key
- **Type**: String
- **Value**: instrumentId (e.g., "KEY000123")
- **Purpose**: Enables partitioning by instrument for ordering guarantees

### Value
- **Format**: JSON
- **Schema**: PriceUpdate object

```json
{
  "instrumentId": "string (KEY\\d{6})",
  "price": "number (double, > 0)",
  "timestamp": "number (long, epoch milliseconds)",
  "bid": "number (double, > 0, bid <= price)",
  "ask": "number (double, > 0, ask >= price)",
  "volume": "number (int, >= 0)"
}
```

### Headers
- **Content-Type**: application/json
- **schema-version**: 1.0.0 (optional, for future versioning)

## Example Message

```json
{
  "key": "KEY000123",
  "value": {
    "instrumentId": "KEY000123",
    "price": 105.75,
    "timestamp": 1696348800000,
    "bid": 105.70,
    "ask": 105.80,
    "volume": 1500
  },
  "headers": {
    "schema-version": "1.0.0"
  }
}
```

## Contract Tests

### Test: Valid PriceUpdate Serialization
```java
@Test
public void testPriceUpdateSerializes() {
    PriceUpdate update = new PriceUpdate(
        "KEY000123", 105.75, 1696348800000L, 105.70, 105.80, 1500
    );
    
    String json = objectMapper.writeValueAsString(update);
    
    assertThat(json).contains("\"instrumentId\":\"KEY000123\"");
    assertThat(json).contains("\"price\":105.75");
}
```

### Test: Valid PriceUpdate Deserialization
```java
@Test
public void testPriceUpdateDeserializes() {
    String json = "{\"instrumentId\":\"KEY000123\",\"price\":105.75," +
                  "\"timestamp\":1696348800000,\"bid\":105.70," +
                  "\"ask\":105.80,\"volume\":1500}";
    
    PriceUpdate update = objectMapper.readValue(json, PriceUpdate.class);
    
    assertThat(update.instrumentId()).isEqualTo("KEY000123");
    assertThat(update.price()).isEqualTo(105.75);
    assertThat(update.volume()).isEqualTo(1500);
}
```

### Test: Kafka Producer-Consumer Roundtrip
```java
@SpringBootTest
@EmbeddedKafka(topics = "price-updates", partitions = 1)
public class PriceUpdateContractTest {
    
    @Autowired
    private KafkaTemplate<String, PriceUpdate> kafkaTemplate;
    
    @Autowired
    private ConsumerFactory<String, PriceUpdate> consumerFactory;
    
    @Test
    public void testProducerConsumerContract() throws Exception {
        // Arrange
        PriceUpdate update = new PriceUpdate(
            "KEY000123", 105.75, System.currentTimeMillis(), 
            105.70, 105.80, 1500
        );
        
        // Act - Produce
        kafkaTemplate.send("price-updates", update.instrumentId(), update)
                     .get(5, TimeUnit.SECONDS);
        
        // Act - Consume
        Consumer<String, PriceUpdate> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("price-updates"));
        
        ConsumerRecords<String, PriceUpdate> records = 
            consumer.poll(Duration.ofSeconds(5));
        
        // Assert
        assertThat(records).hasSize(1);
        ConsumerRecord<String, PriceUpdate> record = records.iterator().next();
        assertThat(record.key()).isEqualTo("KEY000123");
        assertThat(record.value()).isEqualTo(update);
        
        consumer.close();
    }
}
```

### Test: Invalid Instrument ID Rejected
```java
@Test
public void testInvalidInstrumentIdRejected() {
    assertThatThrownBy(() -> new PriceUpdate(
        "INVALID", 105.75, System.currentTimeMillis(), 105.70, 105.80, 1500
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("must match pattern KEY\\d{6}");
}
```

### Test: Price Constraints Validated
```java
@Test
public void testBidCannotExceedPrice() {
    assertThatThrownBy(() -> new PriceUpdate(
        "KEY000123", 105.75, System.currentTimeMillis(), 106.00, 105.80, 1500
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("bid cannot exceed price");
}

@Test
public void testAskCannotBeLessThanPrice() {
    assertThatThrownBy(() -> new PriceUpdate(
        "KEY000123", 105.75, System.currentTimeMillis(), 105.70, 105.00, 1500
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("ask cannot be less than price");
}
```

## Topic Configuration

```yaml
topic: price-updates
partitions: 10
replication-factor: 1  # demo setup, use 3 in production
retention-ms: 86400000  # 24 hours
cleanup-policy: delete
compression-type: snappy
```

## Performance Requirements

- **Throughput**: 1-10 messages/second (demonstration scale)
- **Latency**: p95 < 500ms from producer to consumer
- **Message Size**: ~200 bytes per message (JSON)

## Breaking Change Policy

**MINOR version changes** (backward-compatible):
- Adding optional fields
- Adding new validation that doesn't reject previously valid messages

**MAJOR version changes** (breaking):
- Removing fields
- Renaming fields
- Changing field types
- Tightening validation (e.g., narrowing instrument range)

## Status
- [ ] Contract defined
- [ ] Tests written (must fail before implementation)
- [ ] Tests passing (after implementation)
