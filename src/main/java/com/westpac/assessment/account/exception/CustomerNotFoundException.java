package com.westpac.assessment.account.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(UUID customerId) {
        super(customerId.toString());
    }
}
