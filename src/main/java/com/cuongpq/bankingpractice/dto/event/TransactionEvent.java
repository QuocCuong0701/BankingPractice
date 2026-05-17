package com.cuongpq.bankingpractice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// TransactionEvent.java — đây là "hợp đồng" gửi qua Kafka
// Chú ý: KHÔNG gửi Entity trực tiếp, dùng DTO riêng
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    private String eventId;           // UUID của event
    private String eventType;         // TRANSFER_COMPLETED, DEPOSIT, etc.
    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String fromOwnerName;
    private String toOwnerName;
    private BigDecimal amount;
    private BigDecimal fromNewBalance;
    private String currency;
    private String description;
    private LocalDateTime occurredAt;

    // Version của event schema — để backward compatible khi thay đổi
    private int schemaVersion = 1;
}
