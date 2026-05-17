package com.cuongpq.bankingpractice.messagequeue.consumer;

import com.cuongpq.bankingpractice.config.KafkaTopicConfig;
import com.cuongpq.bankingpractice.dto.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    @KafkaListener(
            topics = KafkaTopicConfig.TOPIC_TRANSACTIONS,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionEvent(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        log.info("Received event: type={} txId={} partition={} offset={}", event.getEventType(),
                event.getTransactionId(), partition, offset);
        try {
            if ("TRANSFER_COMPLETED".equals(event.getEventType())) {
                sendTransferNotification(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process notification for txId={}: {}", event.getTransactionId(), e.getMessage());
            // KHÔNG ack → message sẽ được retry
            // Sau max retry → vào DLQ
            throw e;
        }
    }

    private void sendTransferNotification(TransactionEvent event) {
        // Thực tế: gọi SMS gateway, push notification, email
        // Ở đây mock bằng log
        log.info("[SMS] Gửi đến {}: Tài khoản {} vừa chuyển {} {} đến {}. Số dư còn lại: {}",
                event.getFromOwnerName(),
                event.getFromAccountNumber(),
                event.getAmount(),
                event.getCurrency(),
                event.getToAccountNumber(),
                event.getFromNewBalance()
        );
    }
}
