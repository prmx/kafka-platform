package net.prmx.kafka.platform.subscriber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Price Subscriber Service - Main Application.
 * 
 * This service:
 * - Consumes price updates from the price-updates Kafka topic
 * - Consumes subscription commands from the subscription-commands Kafka topic
 * - Filters price updates based on current subscriptions
 * - Aggregates and logs statistics every 5 seconds
 * 
 * Requires:
 * - Kafka broker accessible at ${spring.kafka.bootstrap-servers}
 * - Topics: price-updates, subscription-commands
 * - kafka-platform-common module for shared models
 */
@SpringBootApplication
@EnableScheduling
public class PriceSubscriberApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceSubscriberApplication.class, args);
    }
}
