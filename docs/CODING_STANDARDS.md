# ATMConnect Coding Standards & Best Practices

## Overview

This document outlines the coding standards, best practices, and architectural principles for the ATMConnect banking application. All contributors must follow these guidelines to ensure code quality, maintainability, and security.

## Table of Contents

1. [Code Structure & Architecture](#code-structure--architecture)
2. [Naming Conventions](#naming-conventions)
3. [Design Patterns](#design-patterns)
4. [Error Handling](#error-handling)
5. [Security Guidelines](#security-guidelines)
6. [Documentation Requirements](#documentation-requirements)
7. [Testing Standards](#testing-standards)
8. [Performance Guidelines](#performance-guidelines)
9. [Code Review Checklist](#code-review-checklist)

## Code Structure & Architecture

### Hexagonal Architecture

```
src/main/java/com/atmconnect/
├── domain/                     # Pure business logic, no dependencies
│   ├── entities/              # Domain entities
│   ├── valueobjects/          # Immutable value objects
│   ├── ports/                 # Interfaces for external dependencies
│   ├── services/              # Domain services and strategies
│   └── constants/             # Domain constants
├── application/               # Use cases and application services
│   ├── usecases/             # Business use case implementations
│   ├── services/             # Application services
│   ├── dto/                  # Data transfer objects
│   └── mappers/              # Entity-DTO mappers
├── infrastructure/           # External concerns
│   ├── adapters/             # Port implementations
│   ├── config/               # Spring configuration
│   ├── security/             # Security implementations
│   └── exceptions/           # Exception handling
└── presentation/             # API layer
    ├── api/                  # REST controllers
    └── dto/                  # API DTOs
```

### Layer Dependencies

- **Domain**: No dependencies on other layers
- **Application**: Only depends on Domain
- **Infrastructure**: Depends on Domain and Application
- **Presentation**: Depends on Domain and Application

## Naming Conventions

### Classes

```java
// Entities: Singular nouns
public class Customer { }
public class Transaction { }

// Value Objects: Descriptive nouns
public class AccountNumber { }
public class Money { }

// Services: Noun + "Service"
public class NotificationService { }
public class CryptoService { }

// Use Cases: Verb + "UseCase"
public class AuthenticationUseCase { }
public class TransactionUseCase { }

// Strategies: Adjective + "Strategy"
public class PinAuthenticationStrategy { }
public class BiometricAuthenticationStrategy { }

// Factories: Noun + "Factory"
public class TransactionFactory { }

// Exceptions: Descriptive + "Exception"
public class ATMConnectException { }
public class InvalidTransactionException { }
```

### Methods

```java
// Business operations: Verbs
public void withdraw(Money amount) { }
public boolean canWithdraw(Money amount) { }

// Queries: get/is/has/can + noun/adjective
public boolean isActive() { }
public boolean hasValidPin() { }
public Money getBalance() { }

// Validation: validate + noun
public void validateWithdrawalEligibility() { }
public void validateAmount(Money amount) { }

// Factory methods: create/build + noun
public Transaction createWithdrawalTransaction() { }
public AuthenticationResult buildSuccessResult() { }
```

### Constants

```java
// All caps with underscores
public static final int MAX_FAILED_ATTEMPTS = 3;
public static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
public static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
```

### Variables

```java
// camelCase, descriptive names
private final TransactionRepository transactionRepository;
private final SecureRandom secureRandom;
private String encryptedPayload;
private LocalDateTime lastWithdrawalDate;
```

## Design Patterns

### Required Patterns

#### Strategy Pattern
Used for authentication methods and transaction processing:

```java
public interface AuthenticationStrategy {
    AuthenticationResult authenticate(Customer customer, AuthenticationCredentials credentials);
    AuthenticationType getAuthenticationType();
}

@Service
public class PinAuthenticationStrategy implements AuthenticationStrategy {
    // Implementation
}
```

#### Factory Pattern
Used for creating complex objects:

```java
@Service
public class TransactionFactory {
    public Transaction createWithdrawalTransaction(Account account, Money amount, ATM atm, String deviceId) {
        // Complex creation logic
    }
}
```

#### Builder Pattern
Used for complex object construction:

```java
Transaction transaction = Transaction.builder()
    .transactionId(new TransactionId())
    .type(TransactionType.WITHDRAWAL)
    .amount(amount)
    .build();
```

#### Repository Pattern
Used for data access abstraction:

```java
public interface CustomerRepository {
    Optional<Customer> findByCustomerNumber(String customerNumber);
    Customer save(Customer customer);
}
```

## Error Handling

### Exception Hierarchy

```java
// Base business exception
public class ATMConnectException extends RuntimeException {
    private final ErrorCode errorCode;
    // Implementation
}

// Specific business exceptions
public class InsufficientFundsException extends ATMConnectException { }
public class AccountLockedException extends ATMConnectException { }
```

### Error Handling Best Practices

```java
// Use specific exceptions
throw new ATMConnectException(ErrorCode.INSUFFICIENT_FUNDS);

// Log before throwing
log.error("Transaction validation failed: {}", validationMessage);
throw new InvalidTransactionException(validationMessage);

// Handle exceptions at service boundaries
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ATMConnectException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(ATMConnectException ex) {
        // Handle and log
    }
}

// Use try-with-resources for resource management
try (var connection = dataSource.getConnection()) {
    // Use connection
} catch (SQLException e) {
    log.error("Database error", e);
    throw new SystemException("Database operation failed");
}
```

## Security Guidelines

### Sensitive Data Handling

```java
// NEVER log sensitive data
log.info("Authentication successful for customer: {}", customerId); // ✅
log.info("PIN verification: {}", pin); // ❌ NEVER

// Mask sensitive data in logs
String maskedPhone = phoneNumber.replaceAll("\\d(?=\\d{4})", "*");
log.info("SMS sent to: {}", maskedPhone); // ✅

// Use constants for security values
private static final int PIN_LENGTH = SecurityConstants.PIN_LENGTH;
```

### Input Validation

```java
// Always validate input at service boundaries
@Override
public void validateCredentials(AuthenticationCredentials credentials) {
    if (credentials == null) {
        throw new IllegalArgumentException("Credentials cannot be null");
    }
    
    if (!PIN_PATTERN.matcher(credentials.getPin()).matches()) {
        throw new IllegalArgumentException("Invalid PIN format");
    }
}

// Use parameterized queries
@Query("SELECT c FROM Customer c WHERE c.customerNumber = :customerNumber")
Optional<Customer> findByCustomerNumber(@Param("customerNumber") String customerNumber);
```

### Cryptographic Operations

```java
// Use secure random
private final SecureRandom secureRandom = new SecureRandom();

// Constant-time comparison
public boolean verifyOTP(String provided, String expected) {
    return MessageDigest.isEqual(provided.getBytes(), expected.getBytes());
}

// Use established algorithms
private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
private static final String HASH_ALGORITHM = "SHA-256";
```

## Documentation Requirements

### Class-Level JavaDoc

```java
/**
 * Domain entity representing a banking transaction in the ATMConnect system.
 * 
 * <p>This entity encapsulates all information related to a banking transaction,
 * including withdrawals, balance inquiries, transfers, and PIN changes.</p>
 * 
 * <h3>Security Features:</h3>
 * <ul>
 *   <li>Unique transaction IDs for tracking and auditing</li>
 *   <li>OTP verification for sensitive operations</li>
 *   <li>Security hash for data integrity verification</li>
 * </ul>
 * 
 * @author ATMConnect Development Team
 * @version 1.0
 * @since 1.0
 */
public class Transaction {
    // Implementation
}
```

### Method-Level JavaDoc

```java
/**
 * Validates that an account can perform a withdrawal operation.
 * 
 * <p>This method performs comprehensive validation including account status,
 * balance verification, and daily limit checks. If any validation fails,
 * an appropriate exception is thrown with a specific error code.</p>
 *
 * @param account the account to validate (must not be null)
 * @param amount the withdrawal amount (must be positive)
 * @throws ATMConnectException if validation fails
 * @throws IllegalArgumentException if parameters are invalid
 * @see SecurityConstants#MAX_DAILY_WITHDRAWAL_DEFAULT
 */
public void validateWithdrawalEligibility(Account account, Money amount) {
    // Implementation
}
```

### Inline Comments

```java
// Complex business logic should be commented
public boolean canWithdraw(Money amount) {
    // Check account status first
    if (!active) {
        return false;
    }
    
    // Verify sufficient balance
    if (balance.isLessThan(amount)) {
        return false;
    }
    
    // Check daily withdrawal limits
    LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
    Money effectiveDailyWithdrawn = (lastWithdrawalDate != null && !lastWithdrawalDate.isBefore(today)) 
        ? dailyWithdrawnAmount 
        : new Money("0", currencyCode);
    
    Money totalWithdrawnToday = effectiveDailyWithdrawn.add(amount);
    return !totalWithdrawnToday.isGreaterThan(dailyWithdrawalLimit);
}
```

## Testing Standards

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class TransactionValidatorTest {
    
    @Mock
    private CryptoService cryptoService;
    
    @InjectMocks
    private TransactionValidator validator;
    
    @Test
    @DisplayName("Should validate withdrawal eligibility for active account with sufficient balance")
    void shouldValidateWithdrawalEligibilityForValidAccount() {
        // Given
        Account account = createValidAccount();
        Money amount = new Money("100.00", "USD");
        
        // When & Then
        assertThatCode(() -> validator.validateWithdrawalEligibility(account, amount))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("Should throw exception for insufficient balance")
    void shouldThrowExceptionForInsufficientBalance() {
        // Given
        Account account = createAccountWithBalance("50.00");
        Money amount = new Money("100.00", "USD");
        
        // When & Then
        assertThatThrownBy(() -> validator.validateWithdrawalEligibility(account, amount))
            .isInstanceOf(ATMConnectException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Transactional
class TransactionUseCaseIntegrationTest {
    
    @Autowired
    private TransactionUseCase transactionUseCase;
    
    @Test
    @DisplayName("Should complete full withdrawal flow with OTP verification")
    void shouldCompleteWithdrawalFlow() {
        // Given
        String accountId = createTestAccount();
        String atmId = createTestATM();
        Money amount = new Money("100.00", "USD");
        
        // When
        Transaction transaction = transactionUseCase.initiateWithdrawal(accountId, amount, atmId, "device-123");
        
        // Then
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transaction.getOtpCode()).isNotNull();
        assertThat(transaction.getReferenceNumber()).isNotNull();
    }
}
```

## Performance Guidelines

### Database Operations

```java
// Use pagination for large result sets
@Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.createdAt DESC")
List<Transaction> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") String accountId, Pageable pageable);

// Use lazy loading for large associations
@OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
private List<Account> accounts;

// Use batch operations
@Modifying
@Query("UPDATE Customer c SET c.lastLoginAt = :timestamp WHERE c.id IN :customerIds")
void updateLastLoginBatch(@Param("customerIds") List<String> customerIds, @Param("timestamp") LocalDateTime timestamp);
```

### Caching

```java
// Cache frequently accessed data
@Cacheable(value = "customers", key = "#customerNumber")
public Optional<Customer> findByCustomerNumber(String customerNumber) {
    return customerRepository.findByCustomerNumber(customerNumber);
}

// Cache with TTL
@Cacheable(value = "otpCodes", key = "#transactionId")
@CacheEvict(value = "otpCodes", key = "#transactionId", condition = "#result.isExpired()")
public OtpValidationResult validateOtp(String transactionId, String otpCode) {
    // Implementation
}
```

### Async Operations

```java
// Use async for non-critical operations
@Async
public CompletableFuture<NotificationResult> sendNotificationAsync(String phoneNumber, String message) {
    return notificationService.sendSms(phoneNumber, message);
}

// Handle async results properly
notificationService.sendOtpSms(phoneNumber, otpCode, reference)
    .whenComplete((result, throwable) -> {
        if (throwable != null) {
            log.error("Notification failed", throwable);
        } else if (!result.isSuccessful()) {
            log.warn("Notification delivery failed: {}", result.getErrorMessage());
        }
    });
```

## Code Review Checklist

### Security Review

- [ ] No sensitive data logged
- [ ] Input validation implemented
- [ ] Proper error handling without information disclosure
- [ ] Secure cryptographic operations
- [ ] Rate limiting considered
- [ ] Authorization checks in place

### Architecture Review

- [ ] Single Responsibility Principle followed
- [ ] Proper layer separation maintained
- [ ] Dependencies flow in correct direction
- [ ] Appropriate design patterns used
- [ ] No circular dependencies

### Code Quality Review

- [ ] Method length under 20 lines
- [ ] Class length under 500 lines
- [ ] Proper naming conventions
- [ ] No magic numbers or strings
- [ ] Adequate test coverage (>90%)
- [ ] Comprehensive JavaDoc

### Performance Review

- [ ] Database queries optimized
- [ ] Appropriate caching strategy
- [ ] Resource management (try-with-resources)
- [ ] No N+1 query problems
- [ ] Async operations for I/O

### Documentation Review

- [ ] Class-level JavaDoc present
- [ ] Method-level JavaDoc for public methods
- [ ] Complex logic commented
- [ ] README updated if needed
- [ ] API documentation current

## Enforcement

These standards are enforced through:

1. **Static Analysis**: Checkstyle, SpotBugs, SonarQube
2. **Build Pipeline**: Quality gates in CI/CD
3. **Code Reviews**: Mandatory peer review
4. **Testing**: Minimum 90% coverage requirement
5. **Documentation**: Automated documentation generation

## References

- [Clean Code by Robert Martin](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350884)
- [Effective Java by Joshua Bloch](https://www.amazon.com/Effective-Java-Joshua-Bloch/dp/0134685997)
- [Spring Framework Documentation](https://spring.io/projects/spring-framework)
- [OWASP Security Guidelines](https://owasp.org/www-project-top-ten/)
- [PCI DSS Standards](https://www.pcisecuritystandards.org/)

---

**Version**: 1.0  
**Last Updated**: 2024  
**Review Cycle**: Quarterly