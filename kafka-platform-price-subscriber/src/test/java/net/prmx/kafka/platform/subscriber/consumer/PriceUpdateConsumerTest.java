package net.prmx.kafka.platform.subscriber.consumer;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import net.prmx.kafka.platform.subscriber.service.PriceFilterService;
import net.prmx.kafka.platform.subscriber.service.StatisticsAggregator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Test for PriceUpdateConsumer - Kafka consumer for price updates with filtering.
 * This test MUST FAIL initially (PriceUpdateConsumer class doesn't exist yet).
 * 
 * Tests verify:
 * - Consumer receives PriceUpdate messages
 * - Filter applied before processing
 * - Statistics updated for matching instruments
 * - Filtered messages are NOT processed
 */
@ExtendWith(MockitoExtension.class)
class PriceUpdateConsumerTest {

    @Mock
    private PriceFilterService filterService;

    @Mock
    private StatisticsAggregator statisticsAggregator;

    @InjectMocks
    private PriceUpdateConsumer priceUpdateConsumer;

    private PriceUpdate testPriceUpdate;

    @BeforeEach
    void setUp() {
        testPriceUpdate = new PriceUpdate(
            "KEY000001",
            100.0,
            Instant.now().toEpochMilli(),
            99.5,
            100.5,
            1000);
    }

    @Test
    void shouldProcessPriceUpdateWhenInstrumentIsSubscribed() {
        // Given: instrument is subscribed
        when(filterService.shouldProcess(testPriceUpdate)).thenReturn(true);

        // When: consumer receives message
        priceUpdateConsumer.consumePriceUpdate(testPriceUpdate);

        // Then: filter checked and statistics updated
        verify(filterService).shouldProcess(testPriceUpdate);
        verify(statisticsAggregator).recordMessage(testPriceUpdate);
    }

    @Test
    void shouldNotProcessPriceUpdateWhenInstrumentIsNotSubscribed() {
        // Given: instrument is not subscribed
        when(filterService.shouldProcess(testPriceUpdate)).thenReturn(false);

        // When: consumer receives message
        priceUpdateConsumer.consumePriceUpdate(testPriceUpdate);

        // Then: filter checked but statistics NOT updated
        verify(filterService).shouldProcess(testPriceUpdate);
        verify(statisticsAggregator, never()).recordMessage(any());
    }

    @Test
    void shouldHandleNullPriceUpdateGracefully() {
        // When: consumer receives null (shouldn't happen, but defensive)
        priceUpdateConsumer.consumePriceUpdate(null);

        // Then: no processing occurs, no exceptions thrown
        verify(filterService, never()).shouldProcess(any());
        verify(statisticsAggregator, never()).recordMessage(any());
    }

    @Test
    void shouldContinueProcessingAfterFilterServiceException() {
        // Given: filter service throws exception
        when(filterService.shouldProcess(testPriceUpdate))
            .thenThrow(new RuntimeException("Filter error"));

        // When/Then: exception should be handled, processing continues
        priceUpdateConsumer.consumePriceUpdate(testPriceUpdate);

        // Statistics should not be updated due to error
        verify(statisticsAggregator, never()).recordMessage(any());
    }

    @Test
    void shouldProcessMultiplePriceUpdates() {
        // Given: multiple price updates
        PriceUpdate update1 = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        PriceUpdate update2 = new PriceUpdate("KEY000002", 200.0, Instant.now().toEpochMilli(), 199.5, 200.5, 2000);
        PriceUpdate update3 = new PriceUpdate("KEY000003", 300.0, Instant.now().toEpochMilli(), 299.5, 300.5, 3000);

        when(filterService.shouldProcess(any())).thenReturn(true, false, true);

        // When: consumer processes all updates
        priceUpdateConsumer.consumePriceUpdate(update1);
        priceUpdateConsumer.consumePriceUpdate(update2);
        priceUpdateConsumer.consumePriceUpdate(update3);

        // Then: only updates 1 and 3 recorded (update 2 filtered out)
        verify(statisticsAggregator).recordMessage(update1);
        verify(statisticsAggregator, never()).recordMessage(update2);
        verify(statisticsAggregator).recordMessage(update3);
    }
}


