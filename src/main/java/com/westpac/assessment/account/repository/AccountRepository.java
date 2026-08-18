package com.westpac.assessment.account.repository;

import com.westpac.assessment.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerIdOrderByAccountNumberAsc(UUID customerId);

    boolean existsByAccountNumber(String accountNumber);

    //TODO - if status support given, we could filter by Active accounts too
    long countByCustomerId(UUID customerId);

    Optional<Account> findTopByCustomerIdOrderByAccountNumberDesc(UUID customerId);
}
