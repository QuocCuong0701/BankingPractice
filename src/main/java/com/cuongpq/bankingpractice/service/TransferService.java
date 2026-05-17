package com.cuongpq.bankingpractice.service;


import com.cuongpq.bankingpractice.dto.request.TransferRequest;
import com.cuongpq.bankingpractice.dto.response.TransferResponse;
import com.cuongpq.bankingpractice.entity.Account;
import com.cuongpq.bankingpractice.entity.Transaction;
import com.cuongpq.bankingpractice.exception.AccountFrozenException;
import com.cuongpq.bankingpractice.exception.AccountNotFoundException;
import com.cuongpq.bankingpractice.exception.InsufficientFundsException;
import com.cuongpq.bankingpractice.exception.SameAccountException;
import com.cuongpq.bankingpractice.messagequeue.producer.TransactionEventProducer;
import com.cuongpq.bankingpractice.repository.AccountRepository;
import com.cuongpq.bankingpractice.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;

    private final TransactionEventProducer eventProducer;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        // 1. Idempotency check — chống trừ tiền 2 lần
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            Optional<Transaction> existing = txRepo.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate request, key: {}", request.getIdempotencyKey());
                return buildResponse(existing.get());
            }
        }

        // 2. Load accounts
        Account from = accountRepo.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Không tìm thấy: " + request.getFromAccountNumber()));
        Account to = accountRepo.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Không tìm thấy: " + request.getToAccountNumber()));

        // 3. Validate
        if (from.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Tài khoản nguồn bị khóa");
        }
        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Số dư không đủ");
        }
        if (from.getId().equals(to.getId())) {
            throw new SameAccountException("Không thể chuyển về cùng tài khoản");
        }

        // 4. Thực hiện chuyển tiền
        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));
        from.setUpdatedAt(LocalDateTime.now());
        to.setUpdatedAt(LocalDateTime.now());

        // @Version tự check: nếu ai đó update cùng account →
        // OptimisticLockException → Spring rollback → client nhận 409
        accountRepo.save(from);
        accountRepo.save(to);

        // 5. Ghi transaction log
        Transaction tx = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .fromAccount(from)
                .toAccount(to)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        txRepo.save(tx);
        log.info("Transfer OK: {} → {} | amount={} | txId={}",
                from.getAccountNumber(), to.getAccountNumber(),
                request.getAmount(), tx.getId());

        // ★ Publish event SAU KHI commit DB thành công
        // Nếu đặt trước save() → DB fail nhưng Kafka đã gửi → consumer xử lý phantom tx
        // Spring @Transactional commit sau khi method return
        // Nên dùng TransactionSynchronizationManager để chắc chắn publish AFTER commit
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventProducer.publishTransferCompleted(tx, from);
                    }
                }
        );

        return buildResponse(tx, from.getBalance());
    }

    private TransferResponse buildResponse(Transaction tx, BigDecimal newBalance) {
        return TransferResponse.builder()
                .transactionId(tx.getId())
                .fromAccount(tx.getFromAccount().getAccountNumber())
                .toAccount(tx.getToAccount().getAccountNumber())
                .amount(tx.getAmount())
                .fromNewBalance(newBalance)
                .timestamp(tx.getCreatedAt())
                .status(tx.getStatus().name())
                .build();
    }

    private TransferResponse buildResponse(Transaction tx) {
        return buildResponse(tx, tx.getFromAccount().getBalance());
    }
}
