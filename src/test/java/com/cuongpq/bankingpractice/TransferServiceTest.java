package com.cuongpq.bankingpractice;

import com.cuongpq.bankingpractice.entity.Account;
import com.cuongpq.bankingpractice.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TransferServiceTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @Transactional
    void shouldAcquireLockSuccessfully() {
        // Given
        Optional<Account> account = accountRepository.findByAccountNumber("ACC001");
        if (account.isEmpty()) {
            return;
        }

        // When
        Optional<Account> lockedAccount = accountRepository.findByIdWithLock(account.get().getId());

        // Then
        assertThat(lockedAccount).isPresent();
        assertThat(lockedAccount.get().getId()).isEqualTo(account.get().getId());
    }
}
