package com.atmconnect.domain.entities;

import com.atmconnect.domain.valueobjects.AccountNumber;
import com.atmconnect.domain.valueobjects.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Currency;

class AccountTest {
    
    private Account account;
    private final Currency USD = Currency.getInstance("USD");
    
    @BeforeEach
    void setUp() {
        account = Account.builder()
            .accountNumber(new AccountNumber("1234567890"))
            .accountType(Account.AccountType.CHECKING)
            .balance(new Money(new BigDecimal("1000.00"), USD))
            .currencyCode("USD")
            .active(true)
            .dailyWithdrawalLimit(new Money(new BigDecimal("500.00"), USD))
            .dailyWithdrawnAmount(new Money(new BigDecimal("0.00"), USD))
            .build();
    }
    
    @Test
    @DisplayName("Should allow withdrawal within balance and daily limit")
    void shouldAllowWithdrawalWithinBalanceAndDailyLimit() {
        Money withdrawAmount = new Money(new BigDecimal("200.00"), USD);
        
        assertThatCode(() -> account.withdraw(withdrawAmount)).doesNotThrowAnyException();
        
        assertThat(account.getBalance().getAmount()).isEqualTo(new BigDecimal("800.00"));
        assertThat(account.getDailyWithdrawnAmount().getAmount()).isEqualTo(new BigDecimal("200.00"));
    }
    
    @Test
    @DisplayName("Should throw exception for withdrawal exceeding balance")
    void shouldThrowExceptionForWithdrawalExceedingBalance() {
        Money withdrawAmount = new Money(new BigDecimal("1500.00"), USD);
        
        assertThatThrownBy(() -> account.withdraw(withdrawAmount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Insufficient balance");
    }
    
    @Test
    @DisplayName("Should throw exception for withdrawal exceeding daily limit")
    void shouldThrowExceptionForWithdrawalExceedingDailyLimit() {
        Money withdrawAmount = new Money(new BigDecimal("600.00"), USD);
        
        assertThatThrownBy(() -> account.withdraw(withdrawAmount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Daily withdrawal limit exceeded");
    }
    
    @Test
    @DisplayName("Should throw exception for withdrawal from inactive account")
    void shouldThrowExceptionForWithdrawalFromInactiveAccount() {
        account.setActive(false);
        Money withdrawAmount = new Money(new BigDecimal("100.00"), USD);
        
        assertThatThrownBy(() -> account.withdraw(withdrawAmount))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Account is not active");
    }
    
    @Test
    @DisplayName("Should allow deposit to active account")
    void shouldAllowDepositToActiveAccount() {
        Money depositAmount = new Money(new BigDecimal("500.00"), USD);
        
        assertThatCode(() -> account.deposit(depositAmount)).doesNotThrowAnyException();
        
        assertThat(account.getBalance().getAmount()).isEqualTo(new BigDecimal("1500.00"));
    }
    
    @Test
    @DisplayName("Should throw exception for deposit to inactive account")
    void shouldThrowExceptionForDepositToInactiveAccount() {
        account.setActive(false);
        Money depositAmount = new Money(new BigDecimal("100.00"), USD);
        
        assertThatThrownBy(() -> account.deposit(depositAmount))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Account is not active");
    }
    
    @Test
    @DisplayName("Should correctly check if withdrawal is allowed")
    void shouldCorrectlyCheckIfWithdrawalIsAllowed() {
        Money validAmount = new Money(new BigDecimal("300.00"), USD);
        Money exceedsBalance = new Money(new BigDecimal("1200.00"), USD);
        Money exceedsLimit = new Money(new BigDecimal("600.00"), USD);
        
        assertThat(account.canWithdraw(validAmount)).isTrue();
        assertThat(account.canWithdraw(exceedsBalance)).isFalse();
        assertThat(account.canWithdraw(exceedsLimit)).isFalse();
    }
    
    @Test
    @DisplayName("Should reset daily withdrawal amount on new day")
    void shouldResetDailyWithdrawalAmountOnNewDay() {
        // Simulate a withdrawal on a previous day
        account.setDailyWithdrawnAmount(new Money(new BigDecimal("400.00"), USD));
        account.setLastWithdrawalDate(java.time.LocalDateTime.now().minusDays(1));
        
        Money withdrawAmount = new Money(new BigDecimal("200.00"), USD);
        
        assertThatCode(() -> account.withdraw(withdrawAmount)).doesNotThrowAnyException();
        
        assertThat(account.getDailyWithdrawnAmount().getAmount()).isEqualTo(new BigDecimal("200.00"));
    }
    
    @Test
    @DisplayName("Should accumulate daily withdrawal amount on same day")
    void shouldAccumulateDailyWithdrawalAmountOnSameDay() {
        Money firstWithdrawal = new Money(new BigDecimal("200.00"), USD);
        Money secondWithdrawal = new Money(new BigDecimal("150.00"), USD);
        
        account.withdraw(firstWithdrawal);
        account.withdraw(secondWithdrawal);
        
        assertThat(account.getDailyWithdrawnAmount().getAmount()).isEqualTo(new BigDecimal("350.00"));
    }
}