package net.prmx.kafka.platform.common.integration;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
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
 * Test configuration for SubscriptionCommand Kafka contract tests.
 */
@TestConfiguration
public class SubscriptionCommandKafkaContractTestConfig {

    @Value("${spring.kafka.bootstrap-servers:${spring.embedded.kafka.brokers}}")
    private String bootstrapServers;

    @Bean
    public String bootstrapServers() {
        return bootstrapServers;
    }

    @Bean
    public ProducerFactory<String, SubscriptionCommand> producerFactory() {
        Map<String, Object> props = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, SubscriptionCommand> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
