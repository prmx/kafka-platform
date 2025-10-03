package net.prmx.kafka.platform.common.util;

import java.util.regex.Pattern;

/**
 * Utility for validating instrument identifiers.
 * Instruments must be in format KEYXXXXXX where XXXXXX is 000000-999999.
 */
public class InstrumentValidator {
    private static final Pattern INSTRUMENT_PATTERN = Pattern.compile("KEY\\d{6}");
    private static final String MIN_INSTRUMENT = "KEY000000";
    private static final String MAX_INSTRUMENT = "KEY999999";

    private InstrumentValidator() {
        // Utility class, prevent instantiation
    }

    /**
     * Check if an instrument ID is valid.
     *
     * @param instrumentId the instrument ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String instrumentId) {
        if (instrumentId == null || !INSTRUMENT_PATTERN.matcher(instrumentId).matches()) {
            return false;
        }
        return instrumentId.compareTo(MIN_INSTRUMENT) >= 0
            && instrumentId.compareTo(MAX_INSTRUMENT) <= 0;
    }

    /**
     * Validate an instrument ID and throw an exception if invalid.
     *
     * @param instrumentId the instrument ID to validate
     * @throws IllegalArgumentException if the instrument ID is invalid
     */
    public static void validateOrThrow(String instrumentId) {
        if (!isValid(instrumentId)) {
            throw new IllegalArgumentException(
                "Invalid instrument ID. Must be in range KEY000000-KEY999999: " + instrumentId);
        }
    }

    /**
     * Format an integer index (0-999999) as an instrument ID.
     *
     * @param index the index to format (0-999999)
     * @return the formatted instrument ID (e.g., KEY000123)
     * @throws IllegalArgumentException if index is out of range
     */
    public static String formatInstrument(int index) {
        if (index < 0 || index > 999999) {
            throw new IllegalArgumentException("Index must be 0-999999: " + index);
        }
        return String.format("KEY%06d", index);
    }
}
