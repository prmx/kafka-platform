package net.prmx.kafka.platform.generator.integration;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for price generator.
 * This test MUST FAIL until the full price generator service is implemented.
 */
@SpringBootTest
@EmbeddedKafka(topics = "price-updates", partitions = 1)
@DirtiesContext
class PriceGeneratorIntegrationTest {

    @Autowired
    private String bootstrapServers;

    @Test
    void testServiceStartsAndGeneratesPrices() throws Exception {
        // Arrange - Create consumer
        Consumer<String, PriceUpdate> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("price-updates"));

        // Act - Wait for messages to be published
        // The service should automatically start generating prices
        Thread.sleep(2000); // Wait 2 seconds for some prices to be generated

        // Poll for messages
        ConsumerRecords<String, PriceUpdate> records = consumer.poll(Duration.ofSeconds(5));

        // Assert
        assertThat(records).isNotEmpty();
        
        // Verify at least one valid price update was received
        records.forEach(record -> {
            assertThat(record.key()).matches("KEY\\d{6}");
            assertThat(record.value()).isNotNull();
            assertThat(record.value().instrumentId()).isEqualTo(record.key());
            assertThat(record.value().price()).isPositive();
        });

        consumer.close();
    }

    @Test
    void testPricesPublishedToCorrectTopic() throws Exception {
        // Arrange
        Consumer<String, PriceUpdate> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("price-updates"));

        // Act
        Thread.sleep(2000);
        ConsumerRecords<String, PriceUpdate> records = consumer.poll(Duration.ofSeconds(5));

        // Assert - Messages are on the correct topic
        assertThat(records).isNotEmpty();
        records.forEach(record -> {
            assertThat(record.topic()).isEqualTo("price-updates");
        });

        consumer.close();
    }

    @Test
    void testGenerationIntervalIsWithinExpectedRange() throws Exception {
        // Arrange
        Consumer<String, PriceUpdate> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("price-updates"));

        // Act - Measure interval between messages
        long startTime = System.currentTimeMillis();
        Thread.sleep(5000); // Wait 5 seconds
        ConsumerRecords<String, PriceUpdate> records = consumer.poll(Duration.ofSeconds(5));
        long endTime = System.currentTimeMillis();

        // Assert - Should have received multiple messages in 5 seconds
        // (at 100ms-1s intervals, we expect 5-50 messages)
        assertThat(records.count()).isGreaterThan(3);
        
        long duration = endTime - startTime;
        double averageInterval = duration / (double) records.count();
        
        // Average interval should be between 100ms and 1000ms
        assertThat(averageInterval).isBetween(100.0, 1000.0);

        consumer.close();
    }

    private Consumer<String, PriceUpdate> createConsumer() {
        Map<String, Object> props = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG, "test-group-generator",
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
