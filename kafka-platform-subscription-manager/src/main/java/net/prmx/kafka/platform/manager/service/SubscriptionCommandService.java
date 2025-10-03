package net.prmx.kafka.platform.manager.service;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import net.prmx.kafka.platform.common.util.InstrumentValidator;
import net.prmx.kafka.platform.manager.producer.SubscriptionCommandProducer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * T043: Business logic for command creation and validation
 */
@Service
public class SubscriptionCommandService {

    private final SubscriptionCommandProducer producer;
    private final RequestSerializationService serializationService;

    public SubscriptionCommandService(SubscriptionCommandProducer producer,
                                      RequestSerializationService serializationService) {
        this.producer = producer;
        this.serializationService = serializationService;
    }

    /**
     * Create a subscription command with validation
     */
    public SubscriptionCommand createCommand(String subscriberId, 
                                              SubscriptionCommand.Action action, 
                                              List<String> instrumentIds) {
        // Validate all instrument IDs
        for (String instrumentId : instrumentIds) {
            InstrumentValidator.validateOrThrow(instrumentId);
        }

        return new SubscriptionCommand(
                subscriberId,
                action.name(),
                instrumentIds,
                Instant.now().toEpochMilli()
        );
    }

    /**
     * Publish a subscription command using serialization service
     */
    public SubscriptionCommand publishCommand(String subscriberId, 
                                SubscriptionCommand.Action action, 
                                List<String> instrumentIds) {
        final SubscriptionCommand[] commandHolder = new SubscriptionCommand[1];
        serializationService.executeSerializedForSubscriber(subscriberId, () -> {
            SubscriptionCommand command = createCommand(subscriberId, action, instrumentIds);
            producer.publishCommand(command);
            commandHolder[0] = command;
        });
        return commandHolder[0];
    }
}
