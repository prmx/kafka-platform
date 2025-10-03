package net.prmx.kafka.platform.generator.service;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import net.prmx.kafka.platform.generator.producer.PriceUpdateProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for PriceGenerationService.
 * This test MUST FAIL until PriceGenerationService is implemented.
 */
@ExtendWith(MockitoExtension.class)
class PriceGenerationServiceTest {

    @Mock
    private PriceUpdateProducer producer;

    @Test
    void testGeneratePriceReturnsValidPriceUpdate() {
        // Arrange
        PriceGenerationService service = new PriceGenerationService(new InstrumentSelector(), producer);
        String instrumentId = "KEY000123";

        // Act
        PriceUpdate priceUpdate = service.generatePrice(instrumentId);

        // Assert
        assertThat(priceUpdate).isNotNull();
        assertThat(priceUpdate.instrumentId()).isEqualTo(instrumentId);
        assertThat(priceUpdate.price()).isPositive();
        assertThat(priceUpdate.timestamp()).isPositive();
        assertThat(priceUpdate.bid()).isPositive();
        assertThat(priceUpdate.ask()).isPositive();
        assertThat(priceUpdate.volume()).isNotNegative();
    }

    @Test
    void testInstrumentIdInValidRange() {
        // Arrange
        PriceGenerationService service = new PriceGenerationService(new InstrumentSelector(), producer);
        String instrumentId = "KEY000456";

        // Act
        PriceUpdate priceUpdate = service.generatePrice(instrumentId);

        // Assert - Instrument should match the provided ID
        assertThat(priceUpdate.instrumentId()).isEqualTo(instrumentId);
        assertThat(instrumentId).matches("KEY\\d{6}");
        int number = Integer.parseInt(instrumentId.substring(3));
        assertThat(number).isBetween(0, 999999);
    }

    @Test
    void testBidLessThanOrEqualToPrice() {
        // Arrange
        PriceGenerationService service = new PriceGenerationService(new InstrumentSelector(), producer);
        String instrumentId = "KEY000789";

        // Act
        PriceUpdate priceUpdate = service.generatePrice(instrumentId);

        // Assert
        assertThat(priceUpdate.bid()).isLessThanOrEqualTo(priceUpdate.price());
    }

    @Test
    void testAskGreaterThanOrEqualToPrice() {
        // Arrange
        PriceGenerationService service = new PriceGenerationService(new InstrumentSelector(), producer);
        String instrumentId = "KEY001234";

        // Act
        PriceUpdate priceUpdate = service.generatePrice(instrumentId);

        // Assert
        assertThat(priceUpdate.ask()).isGreaterThanOrEqualTo(priceUpdate.price());
    }

    @Test
    void testVolumeIsNonNegative() {
        // Arrange
        PriceGenerationService service = new PriceGenerationService(new InstrumentSelector(), producer);
        String instrumentId = "KEY005678";

        // Act
        PriceUpdate priceUpdate = service.generatePrice(instrumentId);

        // Assert
        assertThat(priceUpdate.volume()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testTimestampIsCurrentTime() {
        // Arrange
        PriceGenerationService service = new PriceGenerationService(new InstrumentSelector(), producer);
        String instrumentId = "KEY009999";
        long beforeGeneration = System.currentTimeMillis();

        // Act
        PriceUpdate priceUpdate = service.generatePrice(instrumentId);

        // Assert
        long afterGeneration = System.currentTimeMillis();
        assertThat(priceUpdate.timestamp()).isBetween(beforeGeneration, afterGeneration);
    }
}
