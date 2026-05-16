package com.cuongpq.bankingpractice.controller;

import com.cuongpq.bankingpractice.dto.request.TransferRequest;
import com.cuongpq.bankingpractice.dto.response.TransferResponse;
import com.cuongpq.bankingpractice.repository.AccountRepository;
import com.cuongpq.bankingpractice.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class BankingController {

    private final TransferService transferService;
    private final AccountRepository accountRepo;

    // Chuyển tiền
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        ResponseEntity.internalServerError();
        return ResponseEntity.ok(transferService.transfer(request));
    }

    //Xem số dư
    @GetMapping("/accounts/{accountNumber}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String accountNumber) {
        return accountRepo.findByAccountNumber(accountNumber)
                .map(acc -> ResponseEntity.ok(Map.of(
                        "accountNumber", acc.getAccountNumber(),
                        "ownerName", acc.getOwnerName(),
                        "balance", acc.getBalance(),
                        "currency", acc.getCurrency()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
