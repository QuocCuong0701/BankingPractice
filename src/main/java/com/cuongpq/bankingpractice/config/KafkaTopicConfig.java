package com.cuongpq.bankingpractice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaTopicConfig {

    public static final String TOPIC_TRANSACTIONS = "banking.transactions";
    public static final String TOPIC_TRANSACTIONS_DLQ = "banking.transactions.dlq";

    @Bean
    public NewTopic transactionTopic() {
        return TopicBuilder.name(TOPIC_TRANSACTIONS)
                .partitions(3) // 3 partition → 3 consumer chạy song song tối đa
                .replicas(1) // Local = 1, Production = 3
                .build();
    }

    @Bean
    public NewTopic transactionDlqTopic() {
        return TopicBuilder.name(TOPIC_TRANSACTIONS_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
