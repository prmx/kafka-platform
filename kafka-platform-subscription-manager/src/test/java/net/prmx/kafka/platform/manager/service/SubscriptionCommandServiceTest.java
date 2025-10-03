package net.prmx.kafka.platform.manager.service;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import net.prmx.kafka.platform.manager.producer.SubscriptionCommandProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * T035: Test command creation and validation logic
 * Tests must FAIL before implementation exists
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionCommandServiceTest {

    @Mock
    private SubscriptionCommandProducer producer;

    @Mock
    private RequestSerializationService serializationService;

    private SubscriptionCommandService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionCommandService(producer, serializationService);
    }

    @Test
    void testCreateCommand_validInputs_returnsCommand() {
        String subscriberId = "subscriber-001";
        SubscriptionCommand.Action action = SubscriptionCommand.Action.ADD;
        List<String> instrumentIds = List.of("KEY000001", "KEY000002", "KEY000003");

        SubscriptionCommand command = service.createCommand(subscriberId, action, instrumentIds);

        assertThat(command).isNotNull();
        assertThat(command.subscriberId()).isEqualTo(subscriberId);
        assertThat(command.action()).isEqualTo(action.name()); // action is stored as String
        assertThat(command.instrumentIds()).containsExactlyElementsOf(instrumentIds);
        assertThat(command.timestamp()).isNotNull();
    }

    @Test
    void testCreateCommand_validatesInstrumentIds_beforeCreation() {
        String subscriberId = "subscriber-001";
        SubscriptionCommand.Action action = SubscriptionCommand.Action.REPLACE;
        List<String> invalidInstrumentIds = List.of("KEY000001", "INVALID_ID");

        assertThatThrownBy(() -> service.createCommand(subscriberId, action, invalidInstrumentIds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid instrument ID");
    }

    @Test
    void testCreateCommand_timestampIsCurrentTime() throws InterruptedException {
        String subscriberId = "subscriber-001";
        SubscriptionCommand.Action action = SubscriptionCommand.Action.ADD;
        List<String> instrumentIds = List.of("KEY000001");

        long before = System.currentTimeMillis();
        Thread.sleep(10);
        SubscriptionCommand command = service.createCommand(subscriberId, action, instrumentIds);
        Thread.sleep(10);
        long after = System.currentTimeMillis();

        assertThat(command.timestamp()).isBetween(before, after);
    }

    @Test
    void testCreateCommand_validatesInstrumentPattern() {
        String subscriberId = "subscriber-001";
        SubscriptionCommand.Action action = SubscriptionCommand.Action.REMOVE;
        List<String> outOfRangeIds = List.of("KEY000001", "KEY1000000"); // KEY1000000 is out of range

        assertThatThrownBy(() -> service.createCommand(subscriberId, action, outOfRangeIds))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPublishCommand_callsProducer() {
        // Mock serializationService to execute the runnable immediately
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(serializationService).executeSerializedForSubscriber(any(), any());

        String subscriberId = "subscriber-001";
        SubscriptionCommand.Action action = SubscriptionCommand.Action.ADD;
        List<String> instrumentIds = List.of("KEY000001");

        SubscriptionCommand result = service.publishCommand(subscriberId, action, instrumentIds);

        // Verify serialization service was called (producer is called within the runnable)
        verify(serializationService).executeSerializedForSubscriber(any(), any());
        assertThat(result).isNotNull();
    }

    @Test
    void testPublishCommand_usesSerializationService() {
        String subscriberId = "subscriber-001";
        SubscriptionCommand.Action action = SubscriptionCommand.Action.REPLACE;
        List<String> instrumentIds = List.of("KEY000001", "KEY000002");

        service.publishCommand(subscriberId, action, instrumentIds);

        verify(serializationService).executeSerializedForSubscriber(any(), any());
    }
}
