package net.prmx.kafka.platform.subscriber.config;

import net.prmx.kafka.platform.common.model.PriceUpdate;
import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Kafka consumer configuration for dual consumers.
 * 
 * Configures two separate consumer factories:
 * 1. PriceUpdate consumer - for price-updates topic
 * 2. SubscriptionCommand consumer - for subscription-commands topic (compacted)
 * 
 * Both consumers use the same consumer group to ensure proper coordination.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:price-subscriber-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    /**
     * Consumer factory for PriceUpdate messages.
     */
    @Bean
    public ConsumerFactory<String, PriceUpdate> priceUpdateConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "net.prmx.kafka.platform.common.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PriceUpdate.class.getName());

        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new JsonDeserializer<>(PriceUpdate.class, false)
        );
    }

    /**
     * Listener container factory for PriceUpdate consumer.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PriceUpdate> priceUpdateKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PriceUpdate> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(priceUpdateConsumerFactory());
        return factory;
    }

    /**
     * Consumer factory for SubscriptionCommand messages.
     * Configured to read from beginning for compacted topic state recovery.
     */
    @Bean
    public ConsumerFactory<String, SubscriptionCommand> subscriptionCommandConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Always read from beginning for state
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "net.prmx.kafka.platform.common.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SubscriptionCommand.class.getName());

        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new JsonDeserializer<>(SubscriptionCommand.class, false)
        );
    }

    /**
     * Listener container factory for SubscriptionCommand consumer.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SubscriptionCommand> subscriptionCommandKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SubscriptionCommand> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(subscriptionCommandConsumerFactory());
        return factory;
    }
}
