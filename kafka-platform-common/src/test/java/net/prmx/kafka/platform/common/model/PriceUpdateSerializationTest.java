package net.prmx.kafka.platform.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for PriceUpdate JSON serialization and deserialization.
 * This test MUST FAIL until PriceUpdate is implemented.
 */
class PriceUpdateSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testPriceUpdateSerializes() throws Exception {
        // Arrange
        PriceUpdate update = new PriceUpdate(
            "KEY000123", 105.75, 1696348800000L, 105.70, 105.80, 1500
        );

        // Act
        String json = objectMapper.writeValueAsString(update);

        // Assert
        assertThat(json).contains("\"instrumentId\":\"KEY000123\"");
        assertThat(json).contains("\"price\":105.75");
        assertThat(json).contains("\"timestamp\":1696348800000");
        assertThat(json).contains("\"bid\":105.7"); // JSON may omit trailing zero
        assertThat(json).contains("\"ask\":105.8"); // JSON may omit trailing zero
        assertThat(json).contains("\"volume\":1500");
    }

    @Test
    void testPriceUpdateDeserializes() throws Exception {
        // Arrange
        String json = "{\"instrumentId\":\"KEY000123\",\"price\":105.75," +
                      "\"timestamp\":1696348800000,\"bid\":105.70," +
                      "\"ask\":105.80,\"volume\":1500}";

        // Act
        PriceUpdate update = objectMapper.readValue(json, PriceUpdate.class);

        // Assert
        assertThat(update.instrumentId()).isEqualTo("KEY000123");
        assertThat(update.price()).isEqualTo(105.75);
        assertThat(update.timestamp()).isEqualTo(1696348800000L);
        assertThat(update.bid()).isEqualTo(105.70);
        assertThat(update.ask()).isEqualTo(105.80);
        assertThat(update.volume()).isEqualTo(1500);
    }
}
