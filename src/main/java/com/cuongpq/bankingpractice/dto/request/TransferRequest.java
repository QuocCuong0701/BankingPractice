package com.cuongpq.bankingpractice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Tài khoản nguồn không được để trống")
    private String fromAccountNumber;

    @NotBlank(message = "Tài khoản đích không được để trống")
    private String toAccountNumber;

    @NotNull
    @DecimalMin(value = "1000", message = "Số tiền tối thiểu 1,000 VND")
    private BigDecimal amount;

    private String description;

    // Client tự tạo UUID và gửi lên — server dùng để chống duplicate
    private String idempotencyKey;
}

