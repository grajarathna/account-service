package com.westpac.assessment.account.dto;

import jakarta.validation.constraints.Size;

public record CreateAdditionalAccountRequest(

        @Size(
                min = 5,
                max = 30,
                message = "Account nickname must be between 5 and 30 characters"
        )
        String accountNickname

) {
}
