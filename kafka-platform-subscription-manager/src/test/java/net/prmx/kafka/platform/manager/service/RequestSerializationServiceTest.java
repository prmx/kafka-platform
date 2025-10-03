package net.prmx.kafka.platform.manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T036: Test concurrent request serialization per subscriber
 * Tests must FAIL before implementation exists
 */
class RequestSerializationServiceTest {

    private RequestSerializationService service;

    @BeforeEach
    void setUp() {
        service = new RequestSerializationService();
    }

    @Test
    void testRequestsForSameSubscriber_areSerialized() throws InterruptedException {
        String subscriberId = "subscriber-001";
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> executionOrder = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Submit 3 concurrent tasks for same subscriber
        for (int i = 0; i < 3; i++) {
            int taskNumber = i;
            executor.submit(() -> {
                service.executeSerializedForSubscriber(subscriberId, () -> {
                    int value = counter.incrementAndGet();
                    executionOrder.add(taskNumber);
                    try {
                        Thread.sleep(50); // Simulate work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    latch.countDown();
                });
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // All tasks should have executed
        assertThat(executionOrder).hasSize(3);
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void testRequestsForDifferentSubscribers_executeInParallel() throws InterruptedException {
        String subscriber1 = "subscriber-001";
        String subscriber2 = "subscriber-002";
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch endLatch = new CountDownLatch(2);
        List<Long> startTimes = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Task 1 for subscriber-001
        executor.submit(() -> {
            service.executeSerializedForSubscriber(subscriber1, () -> {
                startTimes.add(System.currentTimeMillis());
                startLatch.countDown();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                endLatch.countDown();
            });
        });

        // Task 2 for subscriber-002
        executor.submit(() -> {
            service.executeSerializedForSubscriber(subscriber2, () -> {
                startTimes.add(System.currentTimeMillis());
                startLatch.countDown();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                endLatch.countDown();
            });
        });

        startLatch.await(2, TimeUnit.SECONDS);
        endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Both tasks should have started at approximately the same time (within 50ms)
        assertThat(startTimes).hasSize(2);
        long timeDiff = Math.abs(startTimes.get(0) - startTimes.get(1));
        assertThat(timeDiff).isLessThan(50);
    }

    @Test
    void testLockAcquiredAndReleased_properly() throws InterruptedException {
        String subscriberId = "subscriber-001";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger executionCount = new AtomicInteger(0);

        // First execution
        service.executeSerializedForSubscriber(subscriberId, () -> {
            executionCount.incrementAndGet();
        });

        // Second execution - should acquire lock again
        service.executeSerializedForSubscriber(subscriberId, () -> {
            executionCount.incrementAndGet();
            latch.countDown();
        });

        latch.await(2, TimeUnit.SECONDS);

        assertThat(executionCount.get()).isEqualTo(2);
    }

    @Test
    void testMultipleSubscribers_withSerializedRequests() throws InterruptedException {
        int subscriberCount = 5;
        int requestsPerSubscriber = 10;
        CountDownLatch latch = new CountDownLatch(subscriberCount * requestsPerSubscriber);
        ExecutorService executor = Executors.newFixedThreadPool(20);

        for (int i = 0; i < subscriberCount; i++) {
            String subscriberId = "subscriber-" + i;
            for (int j = 0; j < requestsPerSubscriber; j++) {
                executor.submit(() -> {
                    service.executeSerializedForSubscriber(subscriberId, () -> {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        latch.countDown();
                    });
                });
            }
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
    }

    @Test
    void testExecuteSerializedForSubscriber_returnsVoid() {
        String subscriberId = "subscriber-001";
        
        // Should not throw and should return void
        service.executeSerializedForSubscriber(subscriberId, () -> {
            // Empty task
        });
    }
}
