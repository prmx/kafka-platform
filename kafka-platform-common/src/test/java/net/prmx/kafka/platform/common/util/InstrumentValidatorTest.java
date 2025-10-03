package net.prmx.kafka.platform.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test for InstrumentValidator utility.
 * This test MUST FAIL until InstrumentValidator is implemented.
 */
class InstrumentValidatorTest {

    @Test
    void testIsValidReturnsTrueForMinimumKey() {
        assertThat(InstrumentValidator.isValid("KEY000000")).isTrue();
    }

    @Test
    void testIsValidReturnsTrueForMaximumKey() {
        assertThat(InstrumentValidator.isValid("KEY999999")).isTrue();
    }

    @Test
    void testIsValidReturnsFalseForOutOfRangeKey() {
        assertThat(InstrumentValidator.isValid("KEY1000000")).isFalse();
    }

    @Test
    void testIsValidReturnsFalseForInvalidFormat() {
        assertThat(InstrumentValidator.isValid("INVALID")).isFalse();
        assertThat(InstrumentValidator.isValid("KEY12345")).isFalse(); // Too short
        assertThat(InstrumentValidator.isValid("KEY1234567")).isFalse(); // Too long
        assertThat(InstrumentValidator.isValid("ABC000000")).isFalse(); // Wrong prefix
    }

    @Test
    void testIsValidReturnsFalseForNull() {
        assertThat(InstrumentValidator.isValid(null)).isFalse();
    }

    @Test
    void testValidateOrThrowSucceedsForValidIds() {
        // Should not throw
        InstrumentValidator.validateOrThrow("KEY000000");
        InstrumentValidator.validateOrThrow("KEY999999");
        InstrumentValidator.validateOrThrow("KEY123456");
    }

    @Test
    void testValidateOrThrowFailsForInvalidIds() {
        assertThatThrownBy(() -> InstrumentValidator.validateOrThrow("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid instrument ID");
    }

    @Test
    void testValidateOrThrowFailsForOutOfRange() {
        assertThatThrownBy(() -> InstrumentValidator.validateOrThrow("KEY1000000"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("KEY000000-KEY999999");
    }

    @Test
    void testFormatInstrumentWithZero() {
        assertThat(InstrumentValidator.formatInstrument(0)).isEqualTo("KEY000000");
    }

    @Test
    void testFormatInstrumentWithMaximum() {
        assertThat(InstrumentValidator.formatInstrument(999999)).isEqualTo("KEY999999");
    }

    @Test
    void testFormatInstrumentWithMiddleValue() {
        assertThat(InstrumentValidator.formatInstrument(123)).isEqualTo("KEY000123");
        assertThat(InstrumentValidator.formatInstrument(456789)).isEqualTo("KEY456789");
    }

    @Test
    void testFormatInstrumentRejectsNegative() {
        assertThatThrownBy(() -> InstrumentValidator.formatInstrument(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0-999999");
    }

    @Test
    void testFormatInstrumentRejectsOutOfRange() {
        assertThatThrownBy(() -> InstrumentValidator.formatInstrument(1000000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0-999999");
    }
}
