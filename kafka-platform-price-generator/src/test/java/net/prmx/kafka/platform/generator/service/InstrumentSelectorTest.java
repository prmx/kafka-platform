package net.prmx.kafka.platform.generator.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for InstrumentSelector.
 * This test MUST FAIL until InstrumentSelector is implemented.
 */
class InstrumentSelectorTest {

    @Test
    void testSelectRandomReturnsKeyFormat() {
        // Arrange
        InstrumentSelector selector = new InstrumentSelector();

        // Act
        String instrumentId = selector.selectRandom();

        // Assert
        assertThat(instrumentId).matches("KEY\\d{6}");
    }

    @Test
    void testSelectRandomReturnsValidRange() {
        // Arrange
        InstrumentSelector selector = new InstrumentSelector();

        // Act
        String instrumentId = selector.selectRandom();

        // Assert
        int number = Integer.parseInt(instrumentId.substring(3));
        assertThat(number).isBetween(0, 999999);
    }

    @Test
    void testRepeatedCallsReturnDifferentInstruments() {
        // Arrange
        InstrumentSelector selector = new InstrumentSelector();
        Set<String> instruments = new HashSet<>();

        // Act - Select 100 instruments
        for (int i = 0; i < 100; i++) {
            instruments.add(selector.selectRandom());
        }

        // Assert - Should have multiple different instruments (randomness check)
        assertThat(instruments.size()).isGreaterThan(1);
    }

    @Test
    void testAllReturnedInstrumentsInValidRange() {
        // Arrange
        InstrumentSelector selector = new InstrumentSelector();

        // Act & Assert - Test 1000 selections
        for (int i = 0; i < 1000; i++) {
            String instrumentId = selector.selectRandom();
            assertThat(instrumentId).matches("KEY\\d{6}");
            int number = Integer.parseInt(instrumentId.substring(3));
            assertThat(number).isBetween(0, 999999);
        }
    }
}
