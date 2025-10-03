package net.prmx.kafka.platform.generator.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for integration tests.
 */
@TestConfiguration
public class IntegrationTestConfig {

    @Value("${spring.kafka.bootstrap-servers:${spring.embedded.kafka.brokers}}")
    private String bootstrapServers;

    @Bean
    public String bootstrapServers() {
        return bootstrapServers;
    }
}
