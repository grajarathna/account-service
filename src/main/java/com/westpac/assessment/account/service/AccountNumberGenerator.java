package com.westpac.assessment.account.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final String BANK_CODE = "020";
    //TODO - Accept this from POST request, as branch should change
    private static final String BRANCH_CODE = "1222";

    private final SecureRandom random = new SecureRandom();

    public String generate(int suffix) {
        //TODO - better if this generated from db sequence number, then never duplicates
        int uniqueNumber = random.nextInt(1_000_000, 10_000_000);
        return build(uniqueNumber, suffix);
    }

    public String build(int uniqueNumber, int suffix) {
        return "%s-%s-%07d-%03d".formatted(
                BANK_CODE,
                BRANCH_CODE,
                uniqueNumber,
                suffix
        );
    }

    /**
     * Get number for account initial account creation
     * @param accountNumber
     * @return
     */
    public int getUniqueNumber(String accountNumber) {
        return Integer.parseInt(
                accountNumber.split("-")[2]);
    }

    /**
     * Extract suffix from accountNumber
     * @param accountNumber
     * @return
     */
    public int getSuffix(String accountNumber) {
        return Integer.parseInt(
                accountNumber.split("-")[3]
        );
    }
}
