package net.prmx.kafka.platform.manager.producer;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * T037: Test Kafka producer for subscription commands
 * Tests must FAIL before implementation exists
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionCommandProducerTest {

    @Mock
    private KafkaTemplate<String, SubscriptionCommand> kafkaTemplate;

    private SubscriptionCommandProducer producer;

    private final String topic = "subscription-commands";

    @BeforeEach
    void setUp() {
        producer = new SubscriptionCommandProducer(kafkaTemplate, topic);
    }

    @Test
    void testPublishCommand_sendsToCorrectTopic() {
        SubscriptionCommand command = new SubscriptionCommand(
                "subscriber-001",
                "ADD",
                List.of("KEY000001", "KEY000002"),
                Instant.now().toEpochMilli()
        );

        CompletableFuture<SendResult<String, SubscriptionCommand>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(topic), eq("subscriber-001"), eq(command)))
                .thenReturn(future);

        producer.publishCommand(command);

        verify(kafkaTemplate).send(eq(topic), eq("subscriber-001"), eq(command));
    }

    @Test
    void testPublishCommand_usesSubscriberIdAsKey() {
        SubscriptionCommand command = new SubscriptionCommand(
                "subscriber-999",
                "REPLACE",
                List.of("KEY000001"),
                Instant.now().toEpochMilli()
        );

        CompletableFuture<SendResult<String, SubscriptionCommand>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(topic), eq("subscriber-999"), eq(command)))
                .thenReturn(future);

        producer.publishCommand(command);

        verify(kafkaTemplate).send(eq(topic), eq("subscriber-999"), eq(command));
    }

    @Test
    void testPublishCommand_handlesSuccessCallback() {
        SubscriptionCommand command = new SubscriptionCommand(
                "subscriber-001",
                "ADD",
                List.of("KEY000001"),
                Instant.now().toEpochMilli()
        );

        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, SubscriptionCommand>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionCommand.class)))
                .thenReturn(future);

        producer.publishCommand(command);

        verify(future).whenComplete(any());
    }

    @Test
    void testPublishCommand_handlesErrorWithRetry() {
        SubscriptionCommand command = new SubscriptionCommand(
                "subscriber-001",
                "REMOVE",
                List.of("KEY000001"),
                Instant.now().toEpochMilli()
        );

        CompletableFuture<SendResult<String, SubscriptionCommand>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));

        when(kafkaTemplate.send(eq(topic), eq("subscriber-001"), eq(command)))
                .thenReturn(future);

        producer.publishCommand(command);

        verify(kafkaTemplate, atLeastOnce()).send(eq(topic), eq("subscriber-001"), eq(command));
    }

    @Test
    void testPublishCommand_multipleCommands_allSent() {
        SubscriptionCommand command1 = new SubscriptionCommand(
                "subscriber-001",
                "ADD",
                List.of("KEY000001"),
                Instant.now().toEpochMilli()
        );

        SubscriptionCommand command2 = new SubscriptionCommand(
                "subscriber-002",
                "REPLACE",
                List.of("KEY000002"),
                Instant.now().toEpochMilli()
        );

        CompletableFuture<SendResult<String, SubscriptionCommand>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any(SubscriptionCommand.class)))
                .thenReturn(future);

        producer.publishCommand(command1);
        producer.publishCommand(command2);

        verify(kafkaTemplate).send(eq(topic), eq("subscriber-001"), eq(command1));
        verify(kafkaTemplate).send(eq(topic), eq("subscriber-002"), eq(command2));
    }
}

