package net.prmx.kafka.platform.subscriber.consumer;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import net.prmx.kafka.platform.subscriber.service.SubscriptionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Test for SubscriptionCommandConsumer - Kafka consumer for subscription commands.
 * This test MUST FAIL initially (SubscriptionCommandConsumer class doesn't exist yet).
 * 
 * Tests verify:
 * - Consumer receives SubscriptionCommand messages
 * - Commands passed to SubscriptionManager for processing
 * - All command actions supported (ADD, REMOVE, REPLACE)
 * - Error handling for invalid commands
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionCommandConsumerTest {

    @Mock
    private SubscriptionManager subscriptionManager;

    @InjectMocks
    private SubscriptionCommandConsumer subscriptionCommandConsumer;

    @Test
    void shouldProcessAddCommand() {
        // Given: ADD command
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "ADD",
            List.of("KEY000001", "KEY000002"),
            Instant.now().toEpochMilli()
        );

        // When: consumer receives command
        subscriptionCommandConsumer.consumeSubscriptionCommand(command);

        // Then: manager processes command
        verify(subscriptionManager).applyCommand(command);
    }

    @Test
    void shouldProcessRemoveCommand() {
        // Given: REMOVE command
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "REMOVE",
            List.of("KEY000001"),
            Instant.now().toEpochMilli()
        );

        // When: consumer receives command
        subscriptionCommandConsumer.consumeSubscriptionCommand(command);

        // Then: manager processes command
        verify(subscriptionManager).applyCommand(command);
    }

    @Test
    void shouldProcessReplaceCommand() {
        // Given: REPLACE command
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001", "KEY000002", "KEY000003"),
            Instant.now().toEpochMilli()
        );

        // When: consumer receives command
        subscriptionCommandConsumer.consumeSubscriptionCommand(command);

        // Then: manager processes command
        verify(subscriptionManager).applyCommand(command);
    }

    @Test
    void shouldHandleNullCommandGracefully() {
        // When: consumer receives null
        subscriptionCommandConsumer.consumeSubscriptionCommand(null);

        // Then: no processing occurs, no exceptions thrown
        verify(subscriptionManager, never()).applyCommand(any());
    }

    @Test
    void shouldContinueProcessingAfterManagerException() {
        // Given: manager throws exception
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "ADD",
            List.of("KEY000001"),
            Instant.now().toEpochMilli()
        );
        doThrow(new RuntimeException("Manager error"))
            .when(subscriptionManager).applyCommand(command);

        // When/Then: exception should be handled, no propagation
        subscriptionCommandConsumer.consumeSubscriptionCommand(command);

        // Verify manager was called despite exception
        verify(subscriptionManager).applyCommand(command);
    }

    @Test
    void shouldProcessMultipleCommands() {
        // Given: multiple commands from different subscribers
        SubscriptionCommand command1 = new SubscriptionCommand(
            "subscriber-001", "ADD", List.of("KEY000001"), Instant.now().toEpochMilli()
        );
        SubscriptionCommand command2 = new SubscriptionCommand(
            "subscriber-002", "REPLACE", List.of("KEY000002", "KEY000003"), Instant.now().toEpochMilli()
        );
        SubscriptionCommand command3 = new SubscriptionCommand(
            "subscriber-001", "REMOVE", List.of("KEY000001"), Instant.now().toEpochMilli()
        );

        // When: consumer processes all commands
        subscriptionCommandConsumer.consumeSubscriptionCommand(command1);
        subscriptionCommandConsumer.consumeSubscriptionCommand(command2);
        subscriptionCommandConsumer.consumeSubscriptionCommand(command3);

        // Then: all commands processed
        verify(subscriptionManager, times(3)).applyCommand(any());
        verify(subscriptionManager).applyCommand(command1);
        verify(subscriptionManager).applyCommand(command2);
        verify(subscriptionManager).applyCommand(command3);
    }

    @Test
    void shouldProcessEmptyInstrumentList() {
        // Given: command with empty instrument list (valid for REMOVE all)
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of(),
            Instant.now().toEpochMilli()
        );

        // When: consumer receives command
        subscriptionCommandConsumer.consumeSubscriptionCommand(command);

        // Then: manager processes command (empty list is valid)
        verify(subscriptionManager).applyCommand(command);
    }
}


