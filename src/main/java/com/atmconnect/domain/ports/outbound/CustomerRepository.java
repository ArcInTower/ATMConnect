package com.atmconnect.domain.ports.outbound;

import com.atmconnect.domain.entities.Customer;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findByCustomerNumber(String customerNumber);
    Optional<Customer> findById(String id);
    Optional<Customer> findByEmail(String email);
    Customer save(Customer customer);
    void delete(Customer customer);
    boolean existsByCustomerNumber(String customerNumber);
    boolean existsByEmail(String email);
}