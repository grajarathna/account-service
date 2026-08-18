package com.westpac.assessment.account.client;

import com.westpac.assessment.account.dto.ProfanityResponse;
import com.westpac.assessment.account.exception.ProfanityServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProfanityClient {

    private final RestClient restClient;

    public ProfanityClient(
            RestClient.Builder restClientBuilder,
            @Value("${external.profanity.base-url}") String baseUrl,
            @Value("${external.profanity.api-key}") String apiKey) {

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
    }

    public boolean containsProfanity(String text) {

        if (text == null || text.isBlank()) {
            return false;
        }

        try {
            ProfanityResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/profanityfilter")
                            .queryParam("text", text)
                            .build())
                    .retrieve()
                    .body(ProfanityResponse.class);

            return response != null
                    && response.hasProfanity();

        } catch (RestClientException ex) {
            throw new ProfanityServiceUnavailableException(
                    "Profanity service is unavailable",
                    ex
            );
        }
    }
}