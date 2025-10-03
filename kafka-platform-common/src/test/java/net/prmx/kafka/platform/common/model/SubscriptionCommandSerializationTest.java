package net.prmx.kafka.platform.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for SubscriptionCommand JSON serialization and deserialization.
 * This test MUST FAIL until SubscriptionCommand is implemented.
 */
class SubscriptionCommandSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSubscriptionCommandSerializes() throws Exception {
        // Arrange
        SubscriptionCommand cmd = new SubscriptionCommand(
            "subscriber-001", "REPLACE",
            List.of("KEY000001", "KEY000050"),
            System.currentTimeMillis()
        );

        // Act
        String json = objectMapper.writeValueAsString(cmd);

        // Assert
        assertThat(json).contains("\"subscriberId\":\"subscriber-001\"");
        assertThat(json).contains("\"action\":\"REPLACE\"");
        assertThat(json).contains("KEY000001");
        assertThat(json).contains("KEY000050");
    }

    @Test
    void testSubscriptionCommandDeserializes() throws Exception {
        // Arrange
        String json = "{\"subscriberId\":\"subscriber-001\"," +
                      "\"action\":\"ADD\",\"instrumentIds\":[\"KEY000001\"]," +
                      "\"timestamp\":1696348800000}";

        // Act
        SubscriptionCommand cmd = objectMapper.readValue(json, SubscriptionCommand.class);

        // Assert
        assertThat(cmd.subscriberId()).isEqualTo("subscriber-001");
        assertThat(cmd.action()).isEqualTo("ADD");
        assertThat(cmd.instrumentIds()).containsExactly("KEY000001");
        assertThat(cmd.timestamp()).isEqualTo(1696348800000L);
    }

    @Test
    void testAllActionTypes() throws Exception {
        // Test ADD
        SubscriptionCommand addCmd = new SubscriptionCommand(
            "sub-1", "ADD", List.of("KEY000001"), System.currentTimeMillis()
        );
        assertThat(objectMapper.writeValueAsString(addCmd)).contains("ADD");

        // Test REMOVE
        SubscriptionCommand removeCmd = new SubscriptionCommand(
            "sub-2", "REMOVE", List.of("KEY000001"), System.currentTimeMillis()
        );
        assertThat(objectMapper.writeValueAsString(removeCmd)).contains("REMOVE");

        // Test REPLACE
        SubscriptionCommand replaceCmd = new SubscriptionCommand(
            "sub-3", "REPLACE", List.of("KEY000001"), System.currentTimeMillis()
        );
        assertThat(objectMapper.writeValueAsString(replaceCmd)).contains("REPLACE");
    }
}
