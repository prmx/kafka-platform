package net.prmx.kafka.platform.manager.dto;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;

import java.util.List;

/**
 * T040: Response DTO for subscription operations
 */
public record SubscriptionResponse(
        String subscriberId,
        SubscriptionCommand.Action action,
        List<String> instrumentIds,
        long timestamp,
        String status
) {
    public static SubscriptionResponse fromCommand(SubscriptionCommand command, String status) {
        return new SubscriptionResponse(
                command.subscriberId(),
                SubscriptionCommand.Action.valueOf(command.action()),
                command.instrumentIds(),
                command.timestamp(),
                status
        );
    }
}
