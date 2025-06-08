package com.atmconnect.infrastructure.adapters.outbound;

import com.atmconnect.domain.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    
    Optional<Account> findByAccountNumber_Value(String accountNumber);
    
    List<Account> findByCustomerId(String customerId);
    
    List<Account> findByCustomerIdAndActiveTrue(String customerId);
    
    @Query("SELECT a FROM Account a WHERE a.customer.id = :customerId AND a.accountType = :accountType")
    List<Account> findByCustomerIdAndAccountType(@Param("customerId") String customerId, 
                                               @Param("accountType") Account.AccountType accountType);
    
    @Query("SELECT a FROM Account a WHERE a.balance.amount > :minBalance AND a.active = true")
    List<Account> findActiveAccountsWithMinBalance(@Param("minBalance") java.math.BigDecimal minBalance);
    
    boolean existsByAccountNumber_Value(String accountNumber);
}