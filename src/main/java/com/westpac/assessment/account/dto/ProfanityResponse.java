package com.westpac.assessment.account.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfanityResponse(

        @JsonProperty("has_profanity")
        boolean hasProfanity

) {
}
