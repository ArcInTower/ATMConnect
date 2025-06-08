package com.atmconnect.domain.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Currency;

class MoneyTest {
    
    private final Currency USD = Currency.getInstance("USD");
    private final Currency EUR = Currency.getInstance("EUR");
    
    @Test
    @DisplayName("Should create money with valid amount and currency")
    void shouldCreateMoneyWithValidAmountAndCurrency() {
        Money money = new Money(new BigDecimal("100.50"), USD);
        
        assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.50"));
        assertThat(money.getCurrency()).isEqualTo(USD);
    }
    
    @Test
    @DisplayName("Should throw exception for negative amount")
    void shouldThrowExceptionForNegativeAmount() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-10.00"), USD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Amount cannot be negative");
    }
    
    @Test
    @DisplayName("Should throw exception for null amount")
    void shouldThrowExceptionForNullAmount() {
        assertThatThrownBy(() -> new Money(null, USD))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Amount cannot be null");
    }
    
    @Test
    @DisplayName("Should add money with same currency")
    void shouldAddMoneyWithSameCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), USD);
        Money money2 = new Money(new BigDecimal("50.00"), USD);
        
        Money result = money1.add(money2);
        
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(result.getCurrency()).isEqualTo(USD);
    }
    
    @Test
    @DisplayName("Should throw exception when adding different currencies")
    void shouldThrowExceptionWhenAddingDifferentCurrencies() {
        Money usdMoney = new Money(new BigDecimal("100.00"), USD);
        Money eurMoney = new Money(new BigDecimal("50.00"), EUR);
        
        assertThatThrownBy(() -> usdMoney.add(eurMoney))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot operate on different currencies");
    }
    
    @Test
    @DisplayName("Should subtract money with same currency")
    void shouldSubtractMoneyWithSameCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), USD);
        Money money2 = new Money(new BigDecimal("30.00"), USD);
        
        Money result = money1.subtract(money2);
        
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("70.00"));
    }
    
    @Test
    @DisplayName("Should throw exception when subtraction results in negative amount")
    void shouldThrowExceptionWhenSubtractionResultsInNegativeAmount() {
        Money money1 = new Money(new BigDecimal("50.00"), USD);
        Money money2 = new Money(new BigDecimal("100.00"), USD);
        
        assertThatThrownBy(() -> money1.subtract(money2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Insufficient funds");
    }
    
    @Test
    @DisplayName("Should compare money amounts correctly")
    void shouldCompareMoneyAmountsCorrectly() {
        Money money1 = new Money(new BigDecimal("100.00"), USD);
        Money money2 = new Money(new BigDecimal("50.00"), USD);
        Money money3 = new Money(new BigDecimal("150.00"), USD);
        
        assertThat(money1.isGreaterThan(money2)).isTrue();
        assertThat(money2.isLessThan(money1)).isTrue();
        assertThat(money3.isGreaterThan(money1)).isTrue();
    }
    
    @Test
    @DisplayName("Should create money from string values")
    void shouldCreateMoneyFromStringValues() {
        Money money = new Money("99.99", "USD");
        
        assertThat(money.getAmount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(money.getCurrency()).isEqualTo(USD);
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
        Money money = new Money(new BigDecimal("100.50"), USD);
        
        assertThat(money.toString()).contains("$").contains("100.50");
    }
}