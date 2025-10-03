package net.prmx.kafka.platform.subscriber.service;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test for PriceFilterService - In-memory Set-based filtering.
 * This test MUST FAIL initially (PriceFilterService class doesn't exist yet).
 * 
 * Tests verify:
 * - shouldProcess() returns true for subscribed instruments
 * - shouldProcess() returns false for non-subscribed instruments
 * - updateSubscriptions() replaces subscription list
 * - Thread-safe operations with concurrent access
 */
class PriceFilterServiceTest {

    private PriceFilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new PriceFilterService();
    }

    @Test
    void shouldReturnFalseWhenNoSubscriptions() {
        // Given: no subscriptions configured
        PriceUpdate update = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);

        // When/Then: should not process any instrument
        assertFalse(filterService.shouldProcess(update));
    }

    @Test
    void shouldReturnTrueForSubscribedInstrument() {
        // Given: instrument is subscribed
        filterService.updateSubscriptions(Set.of("KEY000001", "KEY000002"));
        PriceUpdate update = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);

        // When/Then: should process subscribed instrument
        assertTrue(filterService.shouldProcess(update));
    }

    @Test
    void shouldReturnFalseForNonSubscribedInstrument() {
        // Given: some instruments subscribed
        filterService.updateSubscriptions(Set.of("KEY000001", "KEY000002"));
        PriceUpdate update = new PriceUpdate("KEY000003", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);

        // When/Then: should not process non-subscribed instrument
        assertFalse(filterService.shouldProcess(update));
    }

    @Test
    void shouldReplaceSubscriptionsOnUpdate() {
        // Given: initial subscriptions
        filterService.updateSubscriptions(Set.of("KEY000001", "KEY000002"));
        PriceUpdate update1 = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        PriceUpdate update3 = new PriceUpdate("KEY000003", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);

        assertTrue(filterService.shouldProcess(update1));
        assertFalse(filterService.shouldProcess(update3));

        // When: subscriptions replaced
        filterService.updateSubscriptions(Set.of("KEY000003", "KEY000004"));

        // Then: old subscriptions removed, new ones active
        assertFalse(filterService.shouldProcess(update1));
        assertTrue(filterService.shouldProcess(update3));
    }

    @Test
    void shouldHandleEmptySubscriptionSet() {
        // Given: subscriptions exist
        filterService.updateSubscriptions(Set.of("KEY000001"));
        PriceUpdate update = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        assertTrue(filterService.shouldProcess(update));

        // When: subscriptions cleared
        filterService.updateSubscriptions(Set.of());

        // Then: no instruments should pass filter
        assertFalse(filterService.shouldProcess(update));
    }

    @Test
    void shouldHandleNullPriceUpdateGracefully() {
        // Given: subscriptions configured
        filterService.updateSubscriptions(Set.of("KEY000001"));

        // When/Then: null update should return false (not throw exception)
        assertFalse(filterService.shouldProcess(null));
    }

    @Test
    void shouldBeThreadSafeForConcurrentReads() throws InterruptedException {
        // Given: subscriptions configured
        filterService.updateSubscriptions(Set.of("KEY000001", "KEY000002", "KEY000003"));
        PriceUpdate update = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When: multiple threads read concurrently
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Perform many reads
                    for (int j = 0; j < 1000; j++) {
                        assertTrue(filterService.shouldProcess(update));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: all threads complete without exceptions
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    void shouldBeThreadSafeForConcurrentWritesAndReads() throws InterruptedException {
        // Given: initial subscriptions
        filterService.updateSubscriptions(Set.of("KEY000001"));
        PriceUpdate update1 = new PriceUpdate("KEY000001", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        PriceUpdate update2 = new PriceUpdate("KEY000002", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);

        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When: some threads update, others read concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        // Writer threads
                        for (int j = 0; j < 100; j++) {
                            filterService.updateSubscriptions(Set.of("KEY00000" + (j % 5)));
                        }
                    } else {
                        // Reader threads
                        for (int j = 0; j < 100; j++) {
                            filterService.shouldProcess(update1);
                            filterService.shouldProcess(update2);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: all threads complete without exceptions
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    void shouldHandleLargeSubscriptionSet() {
        // Given: large subscription set (simulate real workload)
        Set<String> instruments = Set.of(
            "KEY000001", "KEY000002", "KEY000003", "KEY000004", "KEY000005",
            "KEY000006", "KEY000007", "KEY000008", "KEY000009", "KEY000010"
        );
        filterService.updateSubscriptions(instruments);

        // When/Then: all subscribed instruments pass filter
        for (String instrumentId : instruments) {
            PriceUpdate update = new PriceUpdate(instrumentId, 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
            assertTrue(filterService.shouldProcess(update));
        }

        // Non-subscribed instrument should not pass
        PriceUpdate nonSubscribed = new PriceUpdate("KEY999999", 100.0, Instant.now().toEpochMilli(), 99.5, 100.5, 1000);
        assertFalse(filterService.shouldProcess(nonSubscribed));
    }
}


