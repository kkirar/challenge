package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Mock
  private NotificationService notificationService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void shouldTransferSuccessfully() {
    AccountsRepository accountRepository = new AccountsRepositoryInMemory();
    accountsService = new AccountsService(accountRepository, notificationService);

    Account from = new Account("Id-1111");
    from.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(from);

    Account to = new Account("Id-2222");
    to.setBalance(new BigDecimal(200));
    this.accountsService.createAccount(to);


    this.accountsService.transfer("Id-1111", "Id-2222", new BigDecimal("30.0"));

    notificationService.notifyAboutTransfer(from,
            "Transferred " + to + " to account " + to.getAccountId());

    notificationService.notifyAboutTransfer(to,
            "Received " + from + " from account " + from.getAccountId());

  }


}
