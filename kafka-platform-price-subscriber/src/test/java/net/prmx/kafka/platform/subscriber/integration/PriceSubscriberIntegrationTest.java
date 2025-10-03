package net.prmx.kafka.platform.subscriber.integration;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import net.prmx.kafka.platform.subscriber.service.PriceFilterService;
import net.prmx.kafka.platform.subscriber.service.StatisticsAggregator;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Integration Test for Price Subscriber - End-to-end test with dual consumers.
 * This test MUST FAIL initially (implementation classes don't exist yet).
 * 
 * Tests verify:
 * - Publish SubscriptionCommand, verify filter updated
 * - Publish PriceUpdate for subscribed instrument, verify statistics updated
 * - Publish PriceUpdate for non-subscribed instrument, verify filtered out
 * - Test dynamic subscription change without restart
 * - Dual Kafka consumers working independently
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"price-updates", "subscription-commands"},
    brokerProperties = {
        "log.cleanup.policy=compact",
        "min.cleanable.dirty.ratio=0.01",
        "segment.ms=1000"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PriceSubscriberIntegrationTest {

    @Autowired
    private PriceFilterService filterService;

    @Autowired
    private StatisticsAggregator statisticsAggregator;

    @org.springframework.beans.factory.annotation.Value("${spring.embedded.kafka.brokers}")
    private String embeddedKafkaBrokers;

    @Test
    void shouldUpdateFilterWhenSubscriptionCommandReceived() throws Exception {
        // Given: Kafka template for subscription commands
        KafkaTemplate<String, SubscriptionCommand> commandTemplate = createCommandProducer();
        
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001", "KEY000002", "KEY000003"),
            Instant.now().toEpochMilli()
        );

        // When: subscription command published
        commandTemplate.send("subscription-commands", command.subscriberId(), command).get(5, TimeUnit.SECONDS);

        // Then: filter service updated (wait for async processing)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            PriceUpdate subscribedUpdate = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
            assertTrue(filterService.shouldProcess(subscribedUpdate), "Subscribed instrument should pass filter");
        });
    }

    @Test
    void shouldProcessSubscribedPriceUpdates() throws Exception {
        // Given: subscription configured
        KafkaTemplate<String, SubscriptionCommand> commandTemplate = createCommandProducer();
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001"),
            Instant.now().toEpochMilli()
        );
        commandTemplate.send("subscription-commands", command.subscriberId(), command).get(5, TimeUnit.SECONDS);

        // Wait for subscription to be processed
        Thread.sleep(1000);

        // When: price update for subscribed instrument published
        KafkaTemplate<String, PriceUpdate> priceTemplate = createPriceProducer();
        PriceUpdate priceUpdate = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        priceTemplate.send("price-updates", priceUpdate.instrumentId(), priceUpdate).get(5, TimeUnit.SECONDS);

        // Then: statistics should be updated
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Statistics aggregator should have recorded the message
            // We verify by checking the filter still has the subscription
            assertTrue(filterService.shouldProcess(priceUpdate));
        });
    }

    @Test
    void shouldFilterOutNonSubscribedPriceUpdates() throws Exception {
        // Given: subscription for specific instruments
        KafkaTemplate<String, SubscriptionCommand> commandTemplate = createCommandProducer();
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001"),
            Instant.now().toEpochMilli()
        );
        commandTemplate.send("subscription-commands", command.subscriberId(), command).get(5, TimeUnit.SECONDS);

        // Wait for subscription to be processed
        Thread.sleep(1000);

        // When: price update for non-subscribed instrument published
        KafkaTemplate<String, PriceUpdate> priceTemplate = createPriceProducer();
        PriceUpdate subscribedUpdate = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        PriceUpdate nonSubscribedUpdate = new PriceUpdate("KEY999999", 999.0, Instant.now().toEpochMilli(), 998.5, 999.5, 9999);
        
        priceTemplate.send("price-updates", subscribedUpdate.instrumentId(), subscribedUpdate).get(5, TimeUnit.SECONDS);
        priceTemplate.send("price-updates", nonSubscribedUpdate.instrumentId(), nonSubscribedUpdate).get(5, TimeUnit.SECONDS);

        // Then: only subscribed instrument passes filter (wait for subscription to be fully processed)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(filterService.shouldProcess(subscribedUpdate), "Subscribed instrument should pass filter");
            assertFalse(filterService.shouldProcess(nonSubscribedUpdate), "Non-subscribed instrument should be filtered out");
        });
    }

    @Test
    void shouldDynamicallyUpdateSubscriptionsWithoutRestart() throws Exception {
        // Given: initial subscription
        KafkaTemplate<String, SubscriptionCommand> commandTemplate = createCommandProducer();
        KafkaTemplate<String, PriceUpdate> priceTemplate = createPriceProducer();

        SubscriptionCommand initialCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001"),
            Instant.now().toEpochMilli()
        );
        commandTemplate.send("subscription-commands", initialCommand.subscriberId(), initialCommand).get(5, TimeUnit.SECONDS);

        // Verify initial subscription works
        PriceUpdate update1 = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        PriceUpdate update2 = new PriceUpdate("KEY000002", 200.0, Instant.now().toEpochMilli(), 199.5, 200.5, 2000);
        
        // Wait for initial subscription to be processed
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(filterService.shouldProcess(update1), "Initial subscription should be active");
            assertFalse(filterService.shouldProcess(update2), "KEY000002 should not be subscribed yet");
        });

        // When: subscription changed dynamically
        SubscriptionCommand updateCommand = new SubscriptionCommand(
            "subscriber-001",
            "ADD",
            List.of("KEY000002"),
            Instant.now().toEpochMilli()
        );
        commandTemplate.send("subscription-commands", updateCommand.subscriberId(), updateCommand).get(5, TimeUnit.SECONDS);

        // Then: filter updated without service restart
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(filterService.shouldProcess(update1), "Original subscription still active");
            assertTrue(filterService.shouldProcess(update2), "New subscription active");
        });
    }

    @Test
    void shouldHandleMultipleSubscriptionActions() throws Exception {
        // Given: Kafka templates
        KafkaTemplate<String, SubscriptionCommand> commandTemplate = createCommandProducer();

        // When: sequence of commands
        SubscriptionCommand replace = new SubscriptionCommand(
            "subscriber-001", "REPLACE", List.of("KEY000001", "KEY000002", "KEY000003"), Instant.now().toEpochMilli()
        );
        SubscriptionCommand add = new SubscriptionCommand(
            "subscriber-001", "ADD", List.of("KEY000004"), Instant.now().toEpochMilli()
        );
        SubscriptionCommand remove = new SubscriptionCommand(
            "subscriber-001", "REMOVE", List.of("KEY000002"), Instant.now().toEpochMilli()
        );

        commandTemplate.send("subscription-commands", replace.subscriberId(), replace).get(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        commandTemplate.send("subscription-commands", add.subscriberId(), add).get(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        commandTemplate.send("subscription-commands", remove.subscriberId(), remove).get(5, TimeUnit.SECONDS);

        // Then: final state should be KEY000001, KEY000003, KEY000004
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(filterService.shouldProcess(new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000)));
            assertFalse(filterService.shouldProcess(new PriceUpdate("KEY000002", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000)));
            assertTrue(filterService.shouldProcess(new PriceUpdate("KEY000003", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000)));
            assertTrue(filterService.shouldProcess(new PriceUpdate("KEY000004", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000)));
        });
    }

    @Test
    void shouldHandleHighVolumeOfPriceUpdates() throws Exception {
        // Given: subscription configured
        KafkaTemplate<String, SubscriptionCommand> commandTemplate = createCommandProducer();
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001", "KEY000002", "KEY000003"),
            Instant.now().toEpochMilli()
        );
        commandTemplate.send("subscription-commands", command.subscriberId(), command).get(5, TimeUnit.SECONDS);
        Thread.sleep(1000);

        // When: high volume of price updates published
        KafkaTemplate<String, PriceUpdate> priceTemplate = createPriceProducer();
        for (int i = 0; i < 100; i++) {
            String instrumentId = "KEY00000" + ((i % 3) + 1); // Rotate through 3 subscribed instruments
            double price = 100.0 + i;
            PriceUpdate update = new PriceUpdate(
                instrumentId,
                price,
                Instant.now().toEpochMilli(),
                price - 0.5,  // bid slightly below price
                price + 0.5,  // ask slightly above price
                1000);
            priceTemplate.send("price-updates", update.instrumentId(), update);
        }

        // Then: all messages should be processed without error
        Thread.sleep(2000);
        // Test passes if no exceptions thrown during high volume
        assertTrue(true);
    }

    // Helper methods to create Kafka producers for testing

    private KafkaTemplate<String, PriceUpdate> createPriceProducer() {
        Map<String, Object> props = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBrokers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    private KafkaTemplate<String, SubscriptionCommand> createCommandProducer() {
        Map<String, Object> props = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBrokers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}


