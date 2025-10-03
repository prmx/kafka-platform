package net.prmx.kafka.platform.generator.config;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for KafkaProducerConfig bean creation.
 * This test MUST FAIL until KafkaProducerConfig is implemented.
 */
@SpringBootTest(classes = {KafkaProducerConfig.class})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092"
})
class KafkaProducerConfigTest {

    @Autowired(required = false)
    private KafkaTemplate<String, PriceUpdate> kafkaTemplate;

    @Test
    void testKafkaTemplateBeansCreated() {
        // Assert
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    void testKafkaTemplateIsConfigured() {
        // Assert - Verify template is properly configured
        assertThat(kafkaTemplate).isNotNull();
        assertThat(kafkaTemplate.getProducerFactory()).isNotNull();
    }
}
