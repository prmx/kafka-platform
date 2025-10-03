package net.prmx.kafka.platform.manager.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * T039: Request DTO for subscription operations
 * Validates instrument IDs match KEY000000-KEY999999 pattern
 */
public record SubscriptionRequest(
        @NotEmpty(message = "Instrument IDs list cannot be empty")
        List<@Pattern(regexp = "KEY\\d{6}", message = "Invalid instrument ID format") String> instrumentIds
) {
}
