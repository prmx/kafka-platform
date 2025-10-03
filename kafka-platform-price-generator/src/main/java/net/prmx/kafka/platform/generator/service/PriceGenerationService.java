package net.prmx.kafka.platform.generator.service;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import net.prmx.kafka.platform.generator.producer.PriceUpdateProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service for generating price updates with guaranteed coverage.
 * 
 * Strategy: Every minute, all 1,000,000 instruments get a price update.
 * Within each minute, updates are published in randomized order at high speed.
 * This ensures subscribers see updates for their instruments quickly.
 */
@Service
public class PriceGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PriceGenerationService.class);
    private static final int TOTAL_INSTRUMENTS = 1_000_000; // KEY000000 to KEY999999
    private static final long CYCLE_DURATION_MS = 60_000; // 1 minute
    private static final int BATCH_SIZE = 1000; // Process instruments in batches
    
    private final InstrumentSelector instrumentSelector;
    private final PriceUpdateProducer producer;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running;
    private int currentCycleCount = 0;

    public PriceGenerationService(InstrumentSelector instrumentSelector, PriceUpdateProducer producer) {
        this.instrumentSelector = instrumentSelector;
        this.producer = producer;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    @PostConstruct
    public void startGeneration() {
        log.info("Starting price generation service - guaranteed coverage mode");
        log.info("Will publish all {} instruments every {} seconds", TOTAL_INSTRUMENTS, CYCLE_DURATION_MS / 1000);
        running = true;
        
        // Start a new cycle immediately and schedule for every minute
        scheduler.scheduleAtFixedRate(this::runCycle, 0, CYCLE_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stopGeneration() {
        log.info("Stopping price generation service");
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Run a complete cycle: publish price updates for all 1 million instruments.
     * Instruments are published in randomized order within the minute.
     */
    private void runCycle() {
        if (!running) {
            return;
        }

        currentCycleCount++;
        long cycleStart = System.currentTimeMillis();
        log.info("Starting price generation cycle #{}", currentCycleCount);

        try {
            // Create list of all instrument indices and shuffle
            List<Integer> indices = new ArrayList<>(TOTAL_INSTRUMENTS);
            for (int i = 0; i < TOTAL_INSTRUMENTS; i++) {
                indices.add(i);
            }
            Collections.shuffle(indices);

            // Calculate delay between messages to spread over the minute
            long delayMicros = (CYCLE_DURATION_MS * 1000) / TOTAL_INSTRUMENTS; // microseconds per message
            
            // Process in batches for efficiency
            int published = 0;
            for (int i = 0; i < TOTAL_INSTRUMENTS && running; i += BATCH_SIZE) {
                int batchEnd = Math.min(i + BATCH_SIZE, TOTAL_INSTRUMENTS);
                List<Integer> batch = indices.subList(i, batchEnd);
                
                for (Integer index : batch) {
                    if (!running) break;
                    
                    String instrumentId = formatInstrumentId(index);
                    PriceUpdate priceUpdate = generatePrice(instrumentId);
                    producer.publishPrice(priceUpdate);
                    published++;
                    
                    // Small delay to spread messages over time (only log every 10000)
                    if (published % 10000 == 0) {
                        log.debug("Published {} / {} prices", published, TOTAL_INSTRUMENTS);
                    }
                }
                
                // Optional: tiny sleep between batches to avoid overwhelming Kafka
                if (batchEnd < TOTAL_INSTRUMENTS) {
                    try {
                        Thread.sleep(1); // 1ms between batches
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            long cycleEnd = System.currentTimeMillis();
            long durationSec = (cycleEnd - cycleStart) / 1000;
            log.info("Completed cycle #{}: published {} price updates in {} seconds ({} msgs/sec)", 
                currentCycleCount, published, durationSec, published / Math.max(durationSec, 1));
                
        } catch (Exception e) {
            log.error("Error in price generation cycle", e);
        }
    }

    /**
     * Format an instrument index as a KEY with leading zeros.
     */
    private String formatInstrumentId(int index) {
        return String.format("KEY%06d", index);
    }

    /**
     * Generate a price update for a specific instrument.
     */
    private PriceUpdate generatePrice(String instrumentId) {
        // Generate realistic price data
        double basePrice = 50.0 + ThreadLocalRandom.current().nextDouble(950.0); // 50-1000
        double spread = basePrice * 0.001; // 0.1% spread
        
        double bid = basePrice - (spread / 2);
        double ask = basePrice + (spread / 2);
        int volume = ThreadLocalRandom.current().nextInt(100, 10001); // 100-10000
        
        return new PriceUpdate(
            instrumentId,
            basePrice,
            System.currentTimeMillis(),
            bid,
            ask,
            volume
        );
    }
}
