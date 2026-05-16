package com.cuongpq.bankingpractice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// TransferResponse.java
@Data
@Builder
public class TransferResponse {
    private UUID transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private BigDecimal fromNewBalance;
    private LocalDateTime timestamp;
    private String status;
}
