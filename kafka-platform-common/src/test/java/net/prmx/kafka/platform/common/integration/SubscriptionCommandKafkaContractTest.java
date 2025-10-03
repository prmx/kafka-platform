package net.prmx.kafka.platform.common.integration;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for SubscriptionCommand Kafka producer-consumer roundtrip with compaction.
 * This test MUST FAIL until SubscriptionCommand and Kafka configuration are implemented.
 */
@SpringBootTest(classes = SubscriptionCommandKafkaContractTestConfig.class)
@EmbeddedKafka(
    topics = "subscription-commands",
    partitions = 1,
    brokerProperties = {
        "log.cleanup.policy=compact",
        "log.segment.bytes=1024",
        "min.cleanable.dirty.ratio=0.01"
    }
)
@DirtiesContext
class SubscriptionCommandKafkaContractTest {

    @Autowired
    private KafkaTemplate<String, SubscriptionCommand> kafkaTemplate;

    @Autowired
    private String bootstrapServers;

    @Test
    void testProducerConsumerRoundtrip() throws Exception {
        // Arrange
        String subscriberId = "subscriber-test-001";
        SubscriptionCommand cmd = new SubscriptionCommand(
            subscriberId, "REPLACE",
            List.of("KEY000001", "KEY000002"),
            System.currentTimeMillis()
        );

        // Create consumer
        Consumer<String, SubscriptionCommand> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("subscription-commands"));

        // Act - Produce (keyed by subscriberId for compaction)
        kafkaTemplate.send("subscription-commands", subscriberId, cmd)
                     .get(5, TimeUnit.SECONDS);

        // Act - Consume
        ConsumerRecords<String, SubscriptionCommand> records =
            consumer.poll(Duration.ofSeconds(10));

        // Assert
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, SubscriptionCommand> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(subscriberId);
        assertThat(record.value()).isNotNull();
        assertThat(record.value().subscriberId()).isEqualTo(subscriberId);
        assertThat(record.value().action()).isEqualTo("REPLACE");
        assertThat(record.value().instrumentIds()).containsExactly("KEY000001", "KEY000002");

        consumer.close();
    }

    @Test
    void testMessageKeyIsSubscriberIdForCompaction() throws Exception {
        // Arrange
        String subscriberId = "subscriber-compaction-test";
        SubscriptionCommand cmd1 = new SubscriptionCommand(
            subscriberId, "REPLACE", List.of("KEY000001"),
            System.currentTimeMillis()
        );
        SubscriptionCommand cmd2 = new SubscriptionCommand(
            subscriberId, "REPLACE", List.of("KEY000002"),
            System.currentTimeMillis() + 1000
        );

        Consumer<String, SubscriptionCommand> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("subscription-commands"));

        // Act - Produce both commands with same key
        kafkaTemplate.send("subscription-commands", subscriberId, cmd1)
                     .get(5, TimeUnit.SECONDS);
        kafkaTemplate.send("subscription-commands", subscriberId, cmd2)
                     .get(5, TimeUnit.SECONDS);

        // Consume all
        ConsumerRecords<String, SubscriptionCommand> records =
            consumer.poll(Duration.ofSeconds(10));

        // Assert - Both messages present (compaction happens later)
        // but both have subscriberId as key
        assertThat(records.count()).isGreaterThanOrEqualTo(1);
        for (ConsumerRecord<String, SubscriptionCommand> record : records) {
            assertThat(record.key()).isEqualTo(subscriberId);
        }

        consumer.close();
    }

    @Test
    void testAllActionTypesSerialize() throws Exception {
        // Test ADD, REMOVE, REPLACE
        Consumer<String, SubscriptionCommand> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("subscription-commands"));

        // Produce all action types
        kafkaTemplate.send("subscription-commands", "sub-add",
            new SubscriptionCommand("sub-add", "ADD", List.of("KEY000001"), System.currentTimeMillis()))
            .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("subscription-commands", "sub-remove",
            new SubscriptionCommand("sub-remove", "REMOVE", List.of("KEY000001"), System.currentTimeMillis()))
            .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("subscription-commands", "sub-replace",
            new SubscriptionCommand("sub-replace", "REPLACE", List.of("KEY000001"), System.currentTimeMillis()))
            .get(5, TimeUnit.SECONDS);

        // Consume
        ConsumerRecords<String, SubscriptionCommand> records =
            consumer.poll(Duration.ofSeconds(10));

        // Assert - All three messages received
        assertThat(records.count()).isEqualTo(3);

        consumer.close();
    }

    private Consumer<String, SubscriptionCommand> createConsumer() {
        Map<String, Object> props = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG, "test-group-commands",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            JsonDeserializer.TRUSTED_PACKAGES, "net.prmx.kafka.platform.common.model",
            JsonDeserializer.VALUE_DEFAULT_TYPE, SubscriptionCommand.class
        );

        ConsumerFactory<String, SubscriptionCommand> consumerFactory =
            new DefaultKafkaConsumerFactory<>(props);
        return consumerFactory.createConsumer();
    }
}
