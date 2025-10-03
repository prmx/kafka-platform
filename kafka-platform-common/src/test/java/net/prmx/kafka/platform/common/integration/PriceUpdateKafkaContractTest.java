package net.prmx.kafka.platform.common.integration;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for PriceUpdate Kafka producer-consumer roundtrip.
 * This test MUST FAIL until PriceUpdate and Kafka configuration are implemented.
 */
@SpringBootTest(classes = PriceUpdateKafkaContractTestConfig.class)
@EmbeddedKafka(topics = "price-updates", partitions = 1)
@DirtiesContext
class PriceUpdateKafkaContractTest {

    @Autowired
    private KafkaTemplate<String, PriceUpdate> kafkaTemplate;

    @Autowired
    private String bootstrapServers;

    @Test
    void testProducerConsumerRoundtrip() throws Exception {
        // Arrange
        PriceUpdate update = new PriceUpdate(
            "KEY000123", 105.75, System.currentTimeMillis(),
            105.70, 105.80, 1500
        );

        // Create consumer
        Consumer<String, PriceUpdate> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("price-updates"));

        // Act - Produce
        kafkaTemplate.send("price-updates", update.instrumentId(), update)
                     .get(5, TimeUnit.SECONDS);

        // Act - Consume
        ConsumerRecords<String, PriceUpdate> records =
            consumer.poll(Duration.ofSeconds(10));

        // Assert
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, PriceUpdate> record = records.iterator().next();
        assertThat(record.key()).isEqualTo("KEY000123");
        assertThat(record.value()).isNotNull();
        assertThat(record.value().instrumentId()).isEqualTo("KEY000123");
        assertThat(record.value().price()).isEqualTo(105.75);
        assertThat(record.value().bid()).isEqualTo(105.70);
        assertThat(record.value().ask()).isEqualTo(105.80);
        assertThat(record.value().volume()).isEqualTo(1500);

        consumer.close();
    }

    @Test
    void testJsonSerializationWorksOverKafka() throws Exception {
        // Arrange - Create update with all fields
        PriceUpdate update = new PriceUpdate(
            "KEY999999", 200.50, System.currentTimeMillis(),
            200.40, 200.60, 5000
        );

        Consumer<String, PriceUpdate> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("price-updates"));

        // Act
        kafkaTemplate.send("price-updates", update.instrumentId(), update)
                     .get(5, TimeUnit.SECONDS);

        ConsumerRecords<String, PriceUpdate> records =
            consumer.poll(Duration.ofSeconds(10));

        // Assert - Verify deserialized object matches
        assertThat(records).hasSize(1);
        PriceUpdate received = records.iterator().next().value();
        assertThat(received).isEqualTo(update);

        consumer.close();
    }

    private Consumer<String, PriceUpdate> createConsumer() {
        Map<String, Object> props = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG, "test-group",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            JsonDeserializer.TRUSTED_PACKAGES, "net.prmx.kafka.platform.common.model",
            JsonDeserializer.VALUE_DEFAULT_TYPE, PriceUpdate.class
        );

        ConsumerFactory<String, PriceUpdate> consumerFactory =
            new DefaultKafkaConsumerFactory<>(props);
        return consumerFactory.createConsumer();
    }
}
