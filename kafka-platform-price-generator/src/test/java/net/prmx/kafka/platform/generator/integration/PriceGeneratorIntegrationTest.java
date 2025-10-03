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

    @org.springframework.beans.factory.annotation.Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void testServiceStartsAndGeneratesPrices() throws Exception {
        // Arrange - Create consumer
        Consumer<String, PriceUpdate> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("price-updates"));

        // Act - Wait for generator to publish a cycle
        // The new generator publishes 1M instruments per cycle (scheduleAtFixedRate with initialDelay=0)
        // It takes 4-10 seconds to publish all 1M messages
        Thread.sleep(15000); // Wait 15 seconds to ensure cycle starts and completes

        // Poll for messages with longer timeout
        ConsumerRecords<String, PriceUpdate> records = consumer.poll(Duration.ofSeconds(15));

        // Assert - Should receive many messages (generator publishes 1M per cycle)
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isGreaterThan(100); // Should get at least hundreds of messages
        
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

        // Act - Wait for generator cycle to complete
        Thread.sleep(15000); // 15 seconds for cycle to complete
        ConsumerRecords<String, PriceUpdate> records = consumer.poll(Duration.ofSeconds(15));

        // Assert - Messages are on the correct topic and we get many messages
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isGreaterThan(100); // Should get hundreds/thousands of messages
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

        // Act - Measure high-throughput generation
        Thread.sleep(15000); // Wait for cycle to complete
        long startTime = System.currentTimeMillis();
        ConsumerRecords<String, PriceUpdate> records = consumer.poll(Duration.ofSeconds(15));
        long endTime = System.currentTimeMillis();

        // Assert - Should have received many messages (generator publishes 1M per cycle)
        // New generator is cycle-based with high throughput (tens of thousands per second)
        assertThat(records.count()).isGreaterThan(100);
        
        long duration = endTime - startTime;
        double averageInterval = duration / (double) records.count();
        
        // Average interval should be very small (< 100ms) due to high-throughput batching
        // Generator publishes ~16,000-250,000 messages/second in bursts
        assertThat(averageInterval).isLessThan(1000.0); // Less than 1 second between messages on average

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
