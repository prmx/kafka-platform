package net.prmx.kafka.platform.generator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for PriceGeneratorApplication.
 * This test MUST FAIL until PriceGeneratorApplication is implemented.
 */
@SpringBootTest
class PriceGeneratorApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Assert - Spring context should load successfully
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void allRequiredBeansAreCreated() {
        // Assert - Verify all required beans exist
        assertThat(applicationContext.containsBean("instrumentSelector")).isTrue();
        assertThat(applicationContext.containsBean("priceGenerationService")).isTrue();
        assertThat(applicationContext.containsBean("priceUpdateProducer")).isTrue();
    }
}
