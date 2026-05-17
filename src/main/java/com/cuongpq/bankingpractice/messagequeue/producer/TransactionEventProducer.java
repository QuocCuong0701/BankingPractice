package com.cuongpq.bankingpractice.messagequeue.producer;

import com.cuongpq.bankingpractice.config.KafkaTopicConfig;
import com.cuongpq.bankingpractice.dto.event.TransactionEvent;
import com.cuongpq.bankingpractice.entity.Account;
import com.cuongpq.bankingpractice.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void publishTransferCompleted(Transaction tx, Account from) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("TRANSFER_COMPLETED")
                .transactionId(tx.getId().toString())
                .fromAccountNumber(from.getAccountNumber())
                .toAccountNumber(tx.getToAccount().getAccountNumber())
                .fromOwnerName(from.getOwnerName())
                .toOwnerName(tx.getToAccount().getOwnerName())
                .amount(tx.getAmount())
                .fromNewBalance(from.getBalance())
                .currency(from.getCurrency())
                .description(tx.getDescription())
                .occurredAt(LocalDateTime.now())
                .build();

        try {
            // Dùng fromAccountNumber làm partition key
            // → Mọi event của cùng 1 account vào cùng 1 partition → đúng thứ tự
            kafkaTemplate.send(
                    KafkaTopicConfig.TOPIC_TRANSACTIONS,
                    from.getAccountNumber(),
                    event
            ).whenComplete((result, ex) -> {
                if (ex != null) {
                    // Producer fail — log lại nhưng KHÔNG throw
                    // Transfer đã thành công rồi, không rollback vì Kafka
                    log.error("Failed to publish event for tx={}: {}", tx.getId(), ex.getMessage());
                    // TODO: lưu vào outbox table để retry sau (Outbox Pattern)
                } else {
                    log.info("Event published: topic={} partition={} offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("publishTransferCompleted: " + e.getMessage());
        }
    }
}
