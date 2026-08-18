package com.westpac.assessment.account.dto;

import java.util.UUID;

public record AccountResponse(
        UUID customerId,
        String accountNumber,
        String customerName,
        String accountNickname
) {
}
