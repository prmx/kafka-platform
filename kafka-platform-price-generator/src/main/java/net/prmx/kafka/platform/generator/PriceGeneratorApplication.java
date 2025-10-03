package net.prmx.kafka.platform.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Price Generator service.
 * Automatically starts generating and publishing price updates on startup.
 */
@SpringBootApplication
public class PriceGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceGeneratorApplication.class, args);
    }
}
