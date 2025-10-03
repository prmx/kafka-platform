package net.prmx.kafka.platform.manager.dto;

import java.time.Instant;

/**
 * T041: Error response DTO for validation failures
 */
public record ErrorResponse(
        String error,
        String message,
        long timestamp
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now().toEpochMilli());
    }
}
