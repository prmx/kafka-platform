package net.prmx.kafka.platform.common.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test for PriceUpdate validation rules.
 * This test MUST FAIL until PriceUpdate is implemented.
 */
class PriceUpdateValidationTest {

    @Test
    void testValidKeyFormatAccepted() {
        // Valid KEY format in range
        new PriceUpdate("KEY000000", 100.0, System.currentTimeMillis(), 99.0, 101.0, 1000);
        new PriceUpdate("KEY999999", 100.0, System.currentTimeMillis(), 99.0, 101.0, 1000);
        new PriceUpdate("KEY123456", 100.0, System.currentTimeMillis(), 99.0, 101.0, 1000);
        // If we get here without exception, test passes
    }

    @Test
    void testInvalidInstrumentIdRejected() {
        assertThatThrownBy(() -> new PriceUpdate(
            "INVALID", 105.75, System.currentTimeMillis(), 105.70, 105.80, 1500
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must match pattern KEY\\d{6}");
    }

    @Test
    void testInstrumentIdOutOfRangeRejected() {
        // KEY1000000 is out of range (>999999)
        assertThatThrownBy(() -> new PriceUpdate(
            "KEY1000000", 105.75, System.currentTimeMillis(), 105.70, 105.80, 1500
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must match pattern");
    }

    @Test
    void testNegativePriceRejected() {
        assertThatThrownBy(() -> new PriceUpdate(
            "KEY000123", -1.0, System.currentTimeMillis(), 105.70, 105.80, 1500
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("price must be positive");
    }

    @Test
    void testBidGreaterThanPriceRejected() {
        assertThatThrownBy(() -> new PriceUpdate(
            "KEY000123", 105.75, System.currentTimeMillis(), 106.00, 105.80, 1500
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bid cannot exceed price");
    }

    @Test
    void testAskLessThanPriceRejected() {
        assertThatThrownBy(() -> new PriceUpdate(
            "KEY000123", 105.75, System.currentTimeMillis(), 105.70, 105.00, 1500
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ask cannot be less than price");
    }

    @Test
    void testNegativeVolumeRejected() {
        assertThatThrownBy(() -> new PriceUpdate(
            "KEY000123", 105.75, System.currentTimeMillis(), 105.70, 105.80, -1
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("volume cannot be negative");
    }

    @Test
    void testNullInstrumentIdRejected() {
        assertThatThrownBy(() -> new PriceUpdate(
            null, 105.75, System.currentTimeMillis(), 105.70, 105.80, 1500
        ))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("instrumentId cannot be null");
    }
}
