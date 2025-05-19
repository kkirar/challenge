package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transfer(String fromId, String toId, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Transfer amount must be positive");
    }

    Account from = accountsRepository.getAccount(fromId);
    Account to = accountsRepository.getAccount(toId);

    if (from == null || to == null) {
      throw new IllegalArgumentException("One of the accounts does not exist");
    }

    // Use String comparison to avoid deadlocks
    Account firstLock = fromId.compareTo(toId) < 0 ? from : to;
    Account secondLock = fromId.compareTo(toId) < 0 ? to : from;

    firstLock.getLock().lock();
    secondLock.getLock().lock();
    try {
      if (from.getBalance().compareTo(amount) < 0) {
        throw new IllegalStateException("Insufficient balance");
      }

      from.debit(amount);
      to.credit(amount);

      notificationService.notifyAboutTransfer(from, "Transferred " + amount + " to account " + to);
      notificationService.notifyAboutTransfer(to, "Received " + amount + " from account " + from);
    } finally {
      secondLock.getLock().unlock();
      firstLock.getLock().unlock();
    }
  }
}
