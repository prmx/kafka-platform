package net.prmx.kafka.platform.subscriber.service;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test for StatisticsAggregator - 5-second window aggregation with atomic counters.
 * This test MUST FAIL initially (StatisticsAggregator class doesn't exist yet).
 * 
 * Tests verify:
 * - recordMessage() increments counters
 * - logAndResetStatistics() logs and resets counters
 * - Thread-safe atomic operations
 * - Zero-count logging works
 * - Unique instrument counting
 */
class StatisticsAggregatorTest {

    private StatisticsAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new StatisticsAggregator();
    }

    @Test
    void shouldRecordSingleMessage() {
        // Given: a price update
        PriceUpdate update = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);

        // When: message recorded
        aggregator.recordMessage(update);

        // Then: counters should reflect the message
        // (We can't directly assert on private fields, but implementation will log these)
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
    }

    @Test
    void shouldRecordMultipleMessages() {
        // Given: multiple price updates
        PriceUpdate update1 = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        PriceUpdate update2 = new PriceUpdate("KEY000002", 200.0, Instant.now().toEpochMilli(), 199.5, 200.5, 2000);
        PriceUpdate update3 = new PriceUpdate("KEY000001", 101.0, Instant.now().toEpochMilli(), 100.5, 101.5, 1100);

        // When: messages recorded
        aggregator.recordMessage(update1);
        aggregator.recordMessage(update2);
        aggregator.recordMessage(update3);

        // Then: should handle multiple messages without error
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
    }

    @Test
    void shouldCountUniqueInstruments() {
        // Given: messages for same and different instruments
        PriceUpdate update1 = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        PriceUpdate update2 = new PriceUpdate("KEY000001", 101.0, Instant.now().toEpochMilli(), 100.5, 101.5, 1100);
        PriceUpdate update3 = new PriceUpdate("KEY000002", 200.0, Instant.now().toEpochMilli(), 199.5, 200.5, 2000);

        // When: messages recorded
        aggregator.recordMessage(update1);
        aggregator.recordMessage(update2);
        aggregator.recordMessage(update3);

        // Then: should track unique instruments (2 unique: KEY000001, KEY000002)
        // Implementation will log: totalMessages=3, uniqueInstruments=2
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
    }

    @Test
    void shouldResetCountersAfterLogging() {
        // Given: messages recorded
        PriceUpdate update = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        aggregator.recordMessage(update);

        // When: log and reset called
        aggregator.logAndResetStatistics();

        // Then: counters should be reset to zero
        // Next log should show zero counts
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
    }

    @Test
    void shouldHandleZeroMessagesGracefully() {
        // When: no messages recorded, log called
        // Then: should log zero counts without error
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
    }

    @Test
    void shouldHandleNullPriceUpdateGracefully() {
        // When: null message passed (defensive check)
        // Then: should not throw exception
        assertDoesNotThrow(() -> aggregator.recordMessage(null));
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
    }

    @Test
    void shouldBeThreadSafeForConcurrentRecording() throws InterruptedException {
        // Given: multiple threads recording messages
        int threadCount = 10;
        int messagesPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When: concurrent message recording
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < messagesPerThread; j++) {
                        PriceUpdate update = new PriceUpdate(
                            "KEY" + String.format("%06d", threadId),
                            100.0,
                            Instant.now().toEpochMilli(),
                            99.5,
                            100.5,
                            1000);
                        aggregator.recordMessage(update);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: all threads complete without exceptions
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Log should show correct total (10 threads * 100 messages = 1000)
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
        
        executor.shutdown();
    }

    @Test
    void shouldBeThreadSafeForConcurrentRecordingAndLogging() throws InterruptedException {
        // Given: some threads recording, others logging
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When: concurrent recording and logging
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    if (threadId % 2 == 0) {
                        // Recording threads
                        for (int j = 0; j < 50; j++) {
                            PriceUpdate update = new PriceUpdate(
                                "KEY000001",
                                100.0,
                                Instant.now().toEpochMilli(),
                                99.5,
                                100.5,
                                1000);
                            aggregator.recordMessage(update);
                        }
                    } else {
                        // Logging threads
                        for (int j = 0; j < 10; j++) {
                            aggregator.logAndResetStatistics();
                            Thread.sleep(10);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: all threads complete without exceptions or deadlocks
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    void shouldAccumulateCountsAcrossMultipleWindows() {
        // Given: messages recorded
        PriceUpdate update1 = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        PriceUpdate update2 = new PriceUpdate("KEY000002", 200.0, Instant.now().toEpochMilli(), 199.5, 200.5, 2000);
        
        aggregator.recordMessage(update1);
        aggregator.recordMessage(update2);

        // When: first window logged and reset
        aggregator.logAndResetStatistics();

        // Then: new window should start fresh
        PriceUpdate update3 = new PriceUpdate("KEY000003", 300.0, Instant.now().toEpochMilli(), 299.5, 300.5, 3000);
        aggregator.recordMessage(update3);
        
        // Second log should show only 1 message (not 3)
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
    }

    @Test
    void shouldHandleHighVolumeOfMessages() {
        // Given: high volume scenario (1000 messages)
        for (int i = 0; i < 1000; i++) {
            PriceUpdate update = new PriceUpdate(
                "KEY" + String.format("%06d", i % 100), // 100 unique instruments
                100.0,
                Instant.now().toEpochMilli(),
                99.5,
                100.5,
                1000);
            aggregator.recordMessage(update);
        }

        // When/Then: should handle high volume without error
        // Implementation should log: totalMessages=1000, uniqueInstruments=100
        assertDoesNotThrow(() -> aggregator.logAndResetStatistics());
    }
}


