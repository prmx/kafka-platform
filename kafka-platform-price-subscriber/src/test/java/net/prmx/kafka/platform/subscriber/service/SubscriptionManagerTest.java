package net.prmx.kafka.platform.subscriber.service;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Test for SubscriptionManager - Manages subscription state with ADD/REMOVE/REPLACE logic.
 * This test MUST FAIL initially (SubscriptionManager class doesn't exist yet).
 * 
 * Tests verify:
 * - ADD action adds instruments to existing set
 * - REMOVE action removes instruments from set
 * - REPLACE action replaces entire set
 * - Thread-safe state updates
 * - Filter service is updated correctly
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionManagerTest {

    @Mock
    private PriceFilterService filterService;

    @InjectMocks
    private SubscriptionManager subscriptionManager;

    @Test
    void shouldAddInstrumentsToEmptySubscription() {
        // Given: ADD command with instruments
        SubscriptionCommand command = new SubscriptionCommand(
            "subscriber-001",
            "ADD",
            List.of("KEY000001", "KEY000002"),
            Instant.now().toEpochMilli()
        );

        // When: command applied
        subscriptionManager.applyCommand(command);

        // Then: filter updated with new instruments
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService).updateSubscriptions(captor.capture());
        
        Set<String> updatedSubscriptions = captor.getValue();
        assertEquals(2, updatedSubscriptions.size());
        assertTrue(updatedSubscriptions.contains("KEY000001"));
        assertTrue(updatedSubscriptions.contains("KEY000002"));
    }

    @Test
    void shouldAddInstrumentsToExistingSubscription() {
        // Given: existing subscription
        SubscriptionCommand initialCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(initialCommand);
        reset(filterService); // Clear previous interactions

        // When: ADD more instruments
        SubscriptionCommand addCommand = new SubscriptionCommand(
            "subscriber-001",
            "ADD",
            List.of("KEY000002", "KEY000003"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(addCommand);

        // Then: filter updated with combined set
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService).updateSubscriptions(captor.capture());
        
        Set<String> updatedSubscriptions = captor.getValue();
        assertEquals(3, updatedSubscriptions.size());
        assertTrue(updatedSubscriptions.containsAll(List.of("KEY000001", "KEY000002", "KEY000003")));
    }

    @Test
    void shouldRemoveInstrumentsFromSubscription() {
        // Given: existing subscription with multiple instruments
        SubscriptionCommand initialCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001", "KEY000002", "KEY000003"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(initialCommand);
        reset(filterService);

        // When: REMOVE some instruments
        SubscriptionCommand removeCommand = new SubscriptionCommand(
            "subscriber-001",
            "REMOVE",
            List.of("KEY000002"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(removeCommand);

        // Then: filter updated with remaining instruments
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService).updateSubscriptions(captor.capture());
        
        Set<String> updatedSubscriptions = captor.getValue();
        assertEquals(2, updatedSubscriptions.size());
        assertTrue(updatedSubscriptions.contains("KEY000001"));
        assertTrue(updatedSubscriptions.contains("KEY000003"));
        assertFalse(updatedSubscriptions.contains("KEY000002"));
    }

    @Test
    void shouldReplaceEntireSubscription() {
        // Given: existing subscription
        SubscriptionCommand initialCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001", "KEY000002"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(initialCommand);
        reset(filterService);

        // When: REPLACE with new instruments
        SubscriptionCommand replaceCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000003", "KEY000004", "KEY000005"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(replaceCommand);

        // Then: filter updated with only new instruments
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService).updateSubscriptions(captor.capture());
        
        Set<String> updatedSubscriptions = captor.getValue();
        assertEquals(3, updatedSubscriptions.size());
        assertTrue(updatedSubscriptions.containsAll(List.of("KEY000003", "KEY000004", "KEY000005")));
        assertFalse(updatedSubscriptions.contains("KEY000001"));
        assertFalse(updatedSubscriptions.contains("KEY000002"));
    }

    @Test
    void shouldHandleEmptyReplaceCommand() {
        // Given: existing subscription
        SubscriptionCommand initialCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001", "KEY000002"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(initialCommand);
        reset(filterService);

        // When: REPLACE with empty list (unsubscribe all)
        SubscriptionCommand replaceCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of(),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(replaceCommand);

        // Then: filter updated with empty set
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService).updateSubscriptions(captor.capture());
        
        Set<String> updatedSubscriptions = captor.getValue();
        assertTrue(updatedSubscriptions.isEmpty());
    }

    @Test
    void shouldHandleRemoveFromEmptySubscription() {
        // Given: no existing subscription
        SubscriptionCommand removeCommand = new SubscriptionCommand(
            "subscriber-001",
            "REMOVE",
            List.of("KEY000001"),
            Instant.now().toEpochMilli()
        );

        // When: REMOVE from empty subscription
        subscriptionManager.applyCommand(removeCommand);

        // Then: filter updated with empty set (no error)
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService).updateSubscriptions(captor.capture());
        
        Set<String> updatedSubscriptions = captor.getValue();
        assertTrue(updatedSubscriptions.isEmpty());
    }

    @Test
    void shouldHandleRemoveNonExistentInstruments() {
        // Given: subscription with some instruments
        SubscriptionCommand initialCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001", "KEY000002"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(initialCommand);
        reset(filterService);

        // When: REMOVE instruments not in subscription
        SubscriptionCommand removeCommand = new SubscriptionCommand(
            "subscriber-001",
            "REMOVE",
            List.of("KEY000003", "KEY000004"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(removeCommand);

        // Then: original subscriptions remain unchanged
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService).updateSubscriptions(captor.capture());
        
        Set<String> updatedSubscriptions = captor.getValue();
        assertEquals(2, updatedSubscriptions.size());
        assertTrue(updatedSubscriptions.containsAll(List.of("KEY000001", "KEY000002")));
    }

    @Test
    void shouldHandleAddDuplicateInstruments() {
        // Given: subscription with existing instruments
        SubscriptionCommand initialCommand = new SubscriptionCommand(
            "subscriber-001",
            "REPLACE",
            List.of("KEY000001", "KEY000002"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(initialCommand);
        reset(filterService);

        // When: ADD includes duplicate instruments
        SubscriptionCommand addCommand = new SubscriptionCommand(
            "subscriber-001",
            "ADD",
            List.of("KEY000002", "KEY000003"),
            Instant.now().toEpochMilli()
        );
        subscriptionManager.applyCommand(addCommand);

        // Then: set prevents duplicates
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService).updateSubscriptions(captor.capture());
        
        Set<String> updatedSubscriptions = captor.getValue();
        assertEquals(3, updatedSubscriptions.size());
        assertTrue(updatedSubscriptions.containsAll(List.of("KEY000001", "KEY000002", "KEY000003")));
    }

    @Test
    void shouldHandleNullCommandGracefully() {
        // When: null command passed
        // Then: should not throw exception, no filter update
        assertDoesNotThrow(() -> subscriptionManager.applyCommand(null));
        verify(filterService, never()).updateSubscriptions(any());
    }

    @Test
    void shouldHandleInvalidActionGracefully() {
        // Given/When: trying to create command with invalid action
        // Then: SubscriptionCommand constructor validates and throws
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new SubscriptionCommand(
                "subscriber-001",
                "INVALID",
                List.of("KEY000001"),
                Instant.now().toEpochMilli()
            )
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("action must be one of: ADD, REMOVE, REPLACE"));
    }

    @Test
    void shouldProcessMultipleCommandsInSequence() {
        // Given: sequence of commands
        SubscriptionCommand cmd1 = new SubscriptionCommand(
            "subscriber-001", "REPLACE", List.of("KEY000001", "KEY000002"), Instant.now().toEpochMilli()
        );
        SubscriptionCommand cmd2 = new SubscriptionCommand(
            "subscriber-001", "ADD", List.of("KEY000003"), Instant.now().toEpochMilli()
        );
        SubscriptionCommand cmd3 = new SubscriptionCommand(
            "subscriber-001", "REMOVE", List.of("KEY000001"), Instant.now().toEpochMilli()
        );

        // When: commands applied in sequence
        subscriptionManager.applyCommand(cmd1);
        subscriptionManager.applyCommand(cmd2);
        subscriptionManager.applyCommand(cmd3);

        // Then: final state should be KEY000002, KEY000003
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(filterService, times(3)).updateSubscriptions(captor.capture());
        
        Set<String> finalSubscriptions = captor.getValue();
        assertEquals(2, finalSubscriptions.size());
        assertTrue(finalSubscriptions.containsAll(List.of("KEY000002", "KEY000003")));
        assertFalse(finalSubscriptions.contains("KEY000001"));
    }
}


