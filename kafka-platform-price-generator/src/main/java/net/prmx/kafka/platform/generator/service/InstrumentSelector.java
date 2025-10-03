package net.prmx.kafka.platform.generator.service;

import net.prmx.kafka.platform.common.util.InstrumentValidator;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for randomly selecting instruments from the 50,000 instrument universe.
 * Uses ThreadLocalRandom for efficient concurrent random number generation.
 */
@Service
public class InstrumentSelector {

    private static final int MAX_INSTRUMENT_INDEX = 999999;

    /**
     * Select a random instrument ID from the valid range (KEY000000-KEY999999).
     *
     * @return a randomly selected instrument ID
     */
    public String selectRandom() {
        int randomIndex = ThreadLocalRandom.current().nextInt(0, MAX_INSTRUMENT_INDEX + 1);
        return InstrumentValidator.formatInstrument(randomIndex);
    }
}
