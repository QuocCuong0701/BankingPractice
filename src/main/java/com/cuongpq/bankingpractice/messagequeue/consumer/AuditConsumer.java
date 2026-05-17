package com.cuongpq.bankingpractice.messagequeue.consumer;

import com.cuongpq.bankingpractice.config.KafkaTopicConfig;
import com.cuongpq.bankingpractice.dto.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditConsumer {

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_TRANSACTIONS, groupId = "audit-service")
    public void handleTransactionEvent(@Payload TransactionEvent event, Acknowledgment ack) {
        try {
            log.info("[Audit] Gửi đến {}: Tài khoản {} vừa chuyển {} {} đến {}. Số dư còn lại: {}",
                    event.getFromOwnerName(),
                    event.getFromAccountNumber(),
                    event.getAmount(),
                    event.getCurrency(),
                    event.getToAccountNumber(),
                    event.getFromNewBalance()
            );
            ack.acknowledge();
        } catch (Exception e) {
            // Log và để retry
            log.error("Audit failed for eventId={}", event.getEventId(), e);
            throw e;
        }
    }
}
