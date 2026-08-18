package com.westpac.assessment.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAccountRequest(
        UUID customerId,

        @NotBlank(message = "Customer name is required")
        @Size(max = 100)
        String customerName,

        @Size(
                min = 5,
                max = 30,
                message = "Account nickname must be between 5 and 30 characters"
        )
        String accountNickname

) {
}
