package com.cuongpq.bankingpractice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 2 request cùng update 1 account → version conflict
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<?> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("Optimistic lock conflict: {}", e.getMessage());
        return ResponseEntity.status(409).body(Map.of(
                "error", "CONFLICT",
                "message", "Giao dịch xung đột, vui lòng thử lại"
        ));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<?> handleInsufficient(InsufficientFundsException e) {
        return ResponseEntity.status(422).body(Map.of(
                "error", "INSUFFICIENT_FUNDS", "message", e.getMessage()));
    }

    @ExceptionHandler(SameAccountException.class)
    public ResponseEntity<?> handleSameAccount(SameAccountException e) {
        return ResponseEntity.status(422).body(Map.of(
                "error", "SAME_ACCOUNT", "message", e.getMessage()));
    }

    @ExceptionHandler(AccountFrozenException.class)
    public ResponseEntity<?> handleAccountFrozen(AccountFrozenException e) {
        return ResponseEntity.status(422).body(Map.of(
                "error", "ACCOUNT_FROZEN", "message", e.getMessage()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<?> handleNotFound(AccountNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of(
                "error", "NOT_FOUND",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_FAILED",
                "details", errors
        ));
    }
}
