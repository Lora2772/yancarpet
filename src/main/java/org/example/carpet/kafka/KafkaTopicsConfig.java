package org.example.carpet.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {
    @Bean
    public NewTopic inventoryReserved() {
        return TopicBuilder.name("inventory.reserved").partitions(1).replicas(1).build();
    }
    @Bean
    public NewTopic paymentSucceeded() {
        return TopicBuilder.name("payment.succeeded").partitions(1).replicas(1).build();
    }
    // 如有其它：inventory.released 等，按需加
}
