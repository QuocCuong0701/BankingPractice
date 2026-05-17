package com.cuongpq.bankingpractice.messagequeue.consumer;

import com.cuongpq.bankingpractice.config.KafkaTopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

// Consumer riêng cho DLQ — alert và xử lý thủ công
@Component
@Slf4j
public class DlqConsumer {

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_TRANSACTIONS_DLQ,
            containerFactory = "dlqKafkaListenerContainerFactory")
    public void handleDlq(@Payload String rawMessage, @Headers Map<String, Object> headers, Acknowledgment ack) {
        try {
            // Gửi alert cho team khi có message vào DLQ
            log.error("[ALERT] Message vào DLQ — cần xử lý thủ công: {}", rawMessage);
            log.error("Original exception: {}", new String((byte[]) headers.get("kafka_dlt-exception-message")));

            // Lưu vào DB để team review
            // alertService.notifySlack(...);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("handleDlq: " + e.getMessage());
            throw e;
        }
    }
}
