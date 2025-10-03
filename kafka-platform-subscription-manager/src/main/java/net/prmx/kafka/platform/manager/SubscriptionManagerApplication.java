package net.prmx.kafka.platform.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * T048: Subscription Manager Application
 * Provides REST API for managing price subscriptions
 */
@SpringBootApplication
public class SubscriptionManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionManagerApplication.class, args);
    }
}
