package com.westpac.assessment.account.exception;

public class AccountNotFoundException extends RuntimeException{

    public AccountNotFoundException(String accountNumber) {
        super(accountNumber);
    }
}
