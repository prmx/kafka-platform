package net.prmx.kafka.platform.common.integration;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Test configuration for PriceUpdate Kafka contract tests.
 */
@TestConfiguration
public class PriceUpdateKafkaContractTestConfig {

    @Value("${spring.kafka.bootstrap-servers:${spring.embedded.kafka.brokers}}")
    private String bootstrapServers;

    @Bean
    public String bootstrapServers() {
        return bootstrapServers;
    }

    @Bean
    public ProducerFactory<String, PriceUpdate> producerFactory() {
        Map<String, Object> props = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, PriceUpdate> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
