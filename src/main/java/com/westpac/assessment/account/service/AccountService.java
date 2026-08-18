package com.westpac.assessment.account.service;

import com.westpac.assessment.account.client.ProfanityClient;
import com.westpac.assessment.account.dto.AccountResponse;
import com.westpac.assessment.account.dto.CreateAccountRequest;
import com.westpac.assessment.account.dto.CreateAdditionalAccountRequest;
import com.westpac.assessment.account.exception.AccountLimitExceededException;
import com.westpac.assessment.account.exception.AccountNotFoundException;
import com.westpac.assessment.account.exception.CustomerNotFoundException;
import com.westpac.assessment.account.exception.InvalidNicknameException;
import com.westpac.assessment.account.model.Account;
import com.westpac.assessment.account.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    
    private static final int MAX_ACCOUNTS = 5;
    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;
    private static final int INITIAL_ACCOUNT_SUFFIX = 0;

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final ProfanityClient profanityClient;


    public AccountService(AccountRepository accountRepository, 
                         AccountNumberGenerator accountNumberGenerator, 
                         ProfanityClient profanityClient) {
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.profanityClient = profanityClient;
    }

    /**
     * Creates a new savings account for a new customer.
     * Generates unique customer ID and account number.
     * 
     * @param request account creation details
     * @return created account details
     * @throws InvalidNicknameException if nickname contains profanity
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        /* TODO: Support an Idempotency-Key for account creation
         *    to prevent duplicate accounts when clients retry timed-out requests.
         *
         * TODO: Current design allows duplicate customers with same name.
         *    Real solution: separate customer registration with unique identifier
         *    (email, phone, national ID) before account creation.
         */
        log.info("Creating new account for customer: {}", request.customerName());

        String sanitizedNickname = sanitizeNickname(request.accountNickname());
        validateNickname(sanitizedNickname);

        UUID customerId = UUID.randomUUID();
        String accountNumber = generateUniqueAccountNumber(INITIAL_ACCOUNT_SUFFIX);

        Account account = buildAccount(
                customerId,
                accountNumber,
                request.customerName().trim(),
                sanitizedNickname
        );

        Account savedAccount = accountRepository.save(account);
        log.info("Successfully created account: {} for customer: {}", 
                savedAccount.getAccountNumber(), customerId);
        
        return toResponse(savedAccount);
    }

    /**
     * Retrieves account details by account number.
     * Results are cached in Redis.
     * 
     * @param accountNumber the account number to look up
     * @return account details
     * @throws AccountNotFoundException if account doesn't exist
     */
    @Cacheable(
            cacheNames = "accounts",
            key = "#accountNumber"
    )
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        return toResponse(account);
    }

    /**
     * Retrieves all accounts for a given customer.
     * Results are cached in Redis.
     * 
     * @param customerId the customer ID
     * @return list of customer's accounts
     * @throws CustomerNotFoundException if customer has no accounts
     */
    /**
     * TODO Create a new DTO with much cleaner response, now duplicate data is there.
     *    customerId
     *    customerName
     *    List<AccountSummary>
     *        AccountSummary :
     *             accountNumber
     *             nickname
     */
    @Cacheable(
            cacheNames = "customerAccounts",
            key = "#customerId"
    )
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(UUID customerId) {
        List<Account> accounts = accountRepository
                .findByCustomerIdOrderByAccountNumberAsc(customerId);

        if (accounts.isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }

        return accounts.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Creates an additional account for an existing customer.
     * Enforces 5-account limit per customer.
     * Account number shares base with customer's existing accounts.
     * 
     * @param customerId the customer ID
     * @param request additional account details
     * @return created account details
     * @throws CustomerNotFoundException if customer doesn't exist
     * @throws AccountLimitExceededException if customer already has 5 accounts
     * @throws InvalidNicknameException if nickname contains profanity
     */
    /* TODO: Replace JVM-level synchronization with a distributed
     *  per-customer lock (e.g. Redis/Redisson) or database locking
     *  for multi-instance production deployments.
     */
    @CacheEvict(
            cacheNames = "customerAccounts",
            key = "#customerId"
    )
    @Transactional
    public synchronized AccountResponse createAdditionalAccount(
            UUID customerId,
            CreateAdditionalAccountRequest request) {
       /*
        * TODO: Support an Idempotency-Key for account creation
        *   to prevent duplicate accounts when clients retry timed-out requests.
        */
        log.info("Creating additional account for customer: {}", customerId);

        String sanitizedNickname = sanitizeNickname(request.accountNickname());
        validateNickname(sanitizedNickname);
        
        long accountCount = accountRepository.countByCustomerId(customerId);
        
        validateCustomerExists(customerId, accountCount);
        validateAccountLimit(accountCount);

        Account lastAccount = findLastCustomerAccount(customerId);
        String accountNumber = generateNextAccountNumber(lastAccount);

        Account account = buildAccount(
                customerId,
                accountNumber,
                lastAccount.getCustomerName(),
                sanitizedNickname
        );

        Account savedAccount = accountRepository.save(account);
        log.info("Successfully created additional account: {} for customer: {}", 
                savedAccount.getAccountNumber(), customerId);
        
        return toResponse(savedAccount);
    }

    private void validateCustomerExists(UUID customerId, long accountCount) {
        if (accountCount == 0) {
            throw new CustomerNotFoundException(customerId);
        }
    }

    private void validateAccountLimit(long accountCount) {
        if (accountCount >= MAX_ACCOUNTS) {
            throw new AccountLimitExceededException(
                    "Customer cannot have more than " + MAX_ACCOUNTS + " accounts"
            );
        }
    }

    private Account findLastCustomerAccount(UUID customerId) {
        return accountRepository
                .findTopByCustomerIdOrderByAccountNumberDesc(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private String generateNextAccountNumber(Account lastAccount) {
        int uniqueNumber = accountNumberGenerator
                .getUniqueNumber(lastAccount.getAccountNumber());
        int nextSuffix = accountNumberGenerator
                .getSuffix(lastAccount.getAccountNumber()) + 1;
        
        return accountNumberGenerator.build(uniqueNumber, nextSuffix);
    }

    private Account buildAccount(UUID customerId, 
                                 String accountNumber, 
                                 String customerName, 
                                 String nickname) {
        return new Account(
                UUID.randomUUID(),
                customerId,
                accountNumber,
                customerName,
                nickname
        );
    }

    private String generateUniqueAccountNumber(int suffix) {
        for (int attempt = 1; attempt <= MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            String accountNumber = accountNumberGenerator.generate(suffix);

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique account number after " + 
                MAX_ACCOUNT_NUMBER_ATTEMPTS + " attempts"
        );
    }

    private String sanitizeNickname(String nickname) {
        if (nickname == null) {
            return null;
        }
        
        String trimmed = nickname.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateNickname(String nickname) {
        if (nickname == null || !StringUtils.hasText(nickname)) {
            return;
        }

        if (profanityClient.containsProfanity(nickname)) {
            throw new InvalidNicknameException(
                    "Account nickname is not permitted"
            );
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getCustomerName(),
                account.getAccountNickname()
        );
    }
}
