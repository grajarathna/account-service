package com.westpac.assessment.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class Account {

    @Id
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "account_nickname")
    private String accountNickname;

    // TODO: Add @Version for optimistic locking when account updates are supported.

    public Account() {

    }

    public Account(UUID id, UUID customerId, String accountNumber, String customerName, String accountNickname) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.accountNickname = accountNickname;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getAccountNickname() {
        return accountNickname;
    }
}
