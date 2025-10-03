package net.prmx.kafka.platform.common.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test for SubscriptionCommand validation rules.
 * This test MUST FAIL until SubscriptionCommand is implemented.
 */
class SubscriptionCommandValidationTest {

    @Test
    void testValidActionsAccepted() {
        // All valid actions should be accepted
        new SubscriptionCommand("sub-1", "ADD", List.of("KEY000001"), System.currentTimeMillis());
        new SubscriptionCommand("sub-2", "REMOVE", List.of("KEY000001"), System.currentTimeMillis());
        new SubscriptionCommand("sub-3", "REPLACE", List.of("KEY000001"), System.currentTimeMillis());
        // If we get here without exception, test passes
    }

    @Test
    void testInvalidActionRejected() {
        assertThatThrownBy(() -> new SubscriptionCommand(
            "subscriber-001", "INVALID_ACTION",
            List.of("KEY000001"), System.currentTimeMillis()
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be one of: ADD, REMOVE, REPLACE");
    }

    @Test
    void testBlankSubscriberIdRejected() {
        assertThatThrownBy(() -> new SubscriptionCommand(
            "", "ADD", List.of("KEY000001"), System.currentTimeMillis()
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be blank");
    }

    @Test
    void testSubscriberIdMaxLengthEnforced() {
        String longId = "a".repeat(101);
        assertThatThrownBy(() -> new SubscriptionCommand(
            longId, "ADD", List.of("KEY000001"), System.currentTimeMillis()
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("max length is 100");
    }

    @Test
    void testInvalidInstrumentIdInListRejected() {
        assertThatThrownBy(() -> new SubscriptionCommand(
            "subscriber-001", "ADD",
            List.of("KEY000001", "INVALID_ID"),
            System.currentTimeMillis()
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid instrumentId");
    }

    @Test
    void testNullSubscriberIdRejected() {
        assertThatThrownBy(() -> new SubscriptionCommand(
            null, "ADD", List.of("KEY000001"), System.currentTimeMillis()
        ))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("subscriberId cannot be null");
    }

    @Test
    void testNullActionRejected() {
        assertThatThrownBy(() -> new SubscriptionCommand(
            "sub-1", null, List.of("KEY000001"), System.currentTimeMillis()
        ))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("action cannot be null");
    }

    @Test
    void testNullInstrumentIdsRejected() {
        assertThatThrownBy(() -> new SubscriptionCommand(
            "sub-1", "ADD", null, System.currentTimeMillis()
        ))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("instrumentIds cannot be null");
    }
}
