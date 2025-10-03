package net.prmx.kafka.platform.generator.producer;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test for PriceUpdateProducer with mocked Kafka.
 * This test MUST FAIL until PriceUpdateProducer is implemented.
 */
@ExtendWith(MockitoExtension.class)
class PriceUpdateProducerTest {

    @Mock
    private KafkaTemplate<String, PriceUpdate> kafkaTemplate;

    private PriceUpdateProducer producer;

    @Test
    void testPublishPriceCallsKafkaTemplateWithCorrectTopicAndKey() {
        // Create producer with explicit topic name
        producer = new PriceUpdateProducer(kafkaTemplate, "price-updates");
        // Arrange
        PriceUpdate priceUpdate = new PriceUpdate(
            "KEY000123", 105.75, System.currentTimeMillis(),
            105.70, 105.80, 1500
        );
        
        CompletableFuture<SendResult<String, PriceUpdate>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(PriceUpdate.class)))
            .thenReturn(future);

        // Act
        producer.publishPrice(priceUpdate);

        // Assert
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PriceUpdate> valueCaptor = ArgumentCaptor.forClass(PriceUpdate.class);
        
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());
        
        assertThat(topicCaptor.getValue()).isEqualTo("price-updates");
        assertThat(keyCaptor.getValue()).isEqualTo("KEY000123");
        assertThat(valueCaptor.getValue()).isEqualTo(priceUpdate);
    }

    @Test
    void testPublishPriceHandlesErrorAndContinues() {
        // Create producer with explicit topic name
        producer = new PriceUpdateProducer(kafkaTemplate, "price-updates");
        
        // Arrange
        PriceUpdate priceUpdate = new PriceUpdate(
            "KEY000123", 105.75, System.currentTimeMillis(),
            105.70, 105.80, 1500
        );
        
        CompletableFuture<SendResult<String, PriceUpdate>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(any(String.class), any(String.class), any(PriceUpdate.class)))
            .thenReturn(future);

        // Act - Should not throw exception
        producer.publishPrice(priceUpdate);

        // Assert - Verify it was called despite error
        verify(kafkaTemplate).send(eq("price-updates"), eq("KEY000123"), eq(priceUpdate));
    }
}
