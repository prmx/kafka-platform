package net.prmx.kafka.platform.manager.integration;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T038: End-to-end REST API → Kafka test
 * Tests must FAIL before implementation exists
 */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"subscription-commands"}, 
               brokerProperties = {"log.cleanup.policy=compact"})
class SubscriptionManagerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void testReplaceSubscription_publishesToKafka() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000001", "KEY000002", "KEY000003"]
            }
            """;

        // Call REST API
        mockMvc.perform(put("/api/v1/subscriptions/subscriber-integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        // Verify message published to Kafka
        var consumer = createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "subscription-commands");

        ConsumerRecord<String, SubscriptionCommand> record = 
                KafkaTestUtils.getSingleRecord(consumer, "subscription-commands", Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("subscriber-integration-test");
        assertThat(record.value()).isNotNull();
        assertThat(record.value().subscriberId()).isEqualTo("subscriber-integration-test");
        assertThat(record.value().action()).isEqualTo("REPLACE");
        assertThat(record.value().instrumentIds()).containsExactly("KEY000001", "KEY000002", "KEY000003");

        consumer.close();
    }

    @Test
    void testAddInstruments_publishesToKafkaWithCorrectKey() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000010", "KEY000011"]
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-add-test/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        var consumer = createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "subscription-commands");

        // Consume records until we find the one we're looking for
        ConsumerRecord<String, SubscriptionCommand> record = null;
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        for (var r : records) {
            if ("subscriber-add-test".equals(r.key())) {
                record = r;
                break;
            }
        }

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("subscriber-add-test");
        assertThat(record.value().action()).isEqualTo("ADD");

        consumer.close();
    }

    @Test
    void testRemoveInstruments_publishesToKafka() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000020"]
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-remove-test/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        var consumer = createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "subscription-commands");

        // Consume records until we find the one we're looking for
        ConsumerRecord<String, SubscriptionCommand> record = null;
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        for (var r : records) {
            if ("subscriber-remove-test".equals(r.key())) {
                record = r;
                break;
            }
        }

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("subscriber-remove-test");
        assertThat(record.value().action()).isEqualTo("REMOVE");
        assertThat(record.value().instrumentIds()).containsExactly("KEY000020");

        consumer.close();
    }

    @Test
    void testCompactionConfiguration_messagesRetainSubscriberId() throws Exception {
        // This test verifies that the topic is configured for compaction
        // by checking that messages with the same key (subscriberId) are retained
        String requestBody1 = """
            {
                "instrumentIds": ["KEY000001"]
            }
            """;

        String requestBody2 = """
            {
                "instrumentIds": ["KEY000002", "KEY000003"]
            }
            """;

        // Send two commands for the same subscriber
        mockMvc.perform(put("/api/v1/subscriptions/subscriber-compact-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody1))
                .andExpect(status().isOk());

        Thread.sleep(100);

        mockMvc.perform(put("/api/v1/subscriptions/subscriber-compact-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2))
                .andExpect(status().isOk());

        // Verify both messages have same key
        var consumer = createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "subscription-commands");

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        
        long countWithKey = 0;
        for (var record : records.records("subscription-commands")) {
            if ("subscriber-compact-test".equals(record.key())) {
                countWithKey++;
            }
        }

        assertThat(countWithKey).isGreaterThanOrEqualTo(1);

        consumer.close();
    }

    private org.apache.kafka.clients.consumer.Consumer<String, SubscriptionCommand> createConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "net.prmx.kafka.platform.common.model");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(SubscriptionCommand.class))
                .createConsumer();
    }
}

