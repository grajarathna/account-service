package com.westpac.assessment.account.service;

import com.westpac.assessment.account.client.ProfanityClient;
import com.westpac.assessment.account.dto.AccountResponse;
import com.westpac.assessment.account.dto.CreateAccountRequest;
import com.westpac.assessment.account.dto.CreateAdditionalAccountRequest;
import com.westpac.assessment.account.exception.AccountLimitExceededException;
import com.westpac.assessment.account.exception.AccountNotFoundException;
import com.westpac.assessment.account.exception.InvalidNicknameException;
import com.westpac.assessment.account.model.Account;
import com.westpac.assessment.account.repository.AccountRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private ProfanityClient profanityClient;

    @InjectMocks
    private AccountService accountService;

    @Nested
    class CreateAccountTests {

        @Test
        void shouldCreateAccountWithValidNickname() {
            // Given
            CreateAccountRequest request = new CreateAccountRequest(null, "Jane Smith", "MySavings");
            String accountNumber = "020-1222-2345678-000";

            when(profanityClient.containsProfanity("MySavings")).thenReturn(false);
            when(accountNumberGenerator.generate(0)).thenReturn(accountNumber);
            when(accountRepository.existsByAccountNumber(accountNumber)).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AccountResponse response = accountService.createAccount(request);

            // Then
            assertThat(response.accountNumber()).isEqualTo(accountNumber);
            assertThat(response.customerName()).isEqualTo("Jane Smith");
            assertThat(response.accountNickname()).isEqualTo("MySavings");
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        void shouldRejectProfaneNickname() {
            // Given
            CreateAccountRequest request = new CreateAccountRequest(null, "John Doe", "BadWord");
            when(profanityClient.containsProfanity("BadWord")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> accountService.createAccount(request))
                    .isInstanceOf(InvalidNicknameException.class)
                    .hasMessage("Account nickname is not permitted");
        }

        // TODO: Test account creation without nickname
        // TODO: Test blank nickname skips profanity check
    }

    @Nested
    class GetAccountTests {

        @Test
        void shouldGetAccountSuccessfully() {
            // Given
            String accountNumber = "020-1222-1234567-000";
            UUID customerId = UUID.randomUUID();
            Account account = new Account(UUID.randomUUID(), customerId, accountNumber, "John Doe", "MySavings");

            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

            // When
            AccountResponse response = accountService.getAccount(accountNumber);

            // Then
            assertThat(response.accountNumber()).isEqualTo(accountNumber);
            assertThat(response.customerName()).isEqualTo("John Doe");
            assertThat(response.accountNickname()).isEqualTo("MySavings");
        }

        @Test
        void shouldThrowExceptionWhenAccountNotFound() {
            // Given
            String accountNumber = "020-1222-9999999-000";
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> accountService.getAccount(accountNumber))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        // TODO: Test retrieving account with null nickname
    }

    @Nested
    class CreateAdditionalAccountTests {

        @Test
        void shouldCreateAdditionalAccountSuccessfully() {
            // Given
            UUID customerId = UUID.randomUUID();
            CreateAdditionalAccountRequest request = new CreateAdditionalAccountRequest("SecondAccount");
            Account existingAccount = new Account(UUID.randomUUID(), customerId, "020-1222-1234567-000", "John Doe", "FirstAccount");

            when(profanityClient.containsProfanity("SecondAccount")).thenReturn(false);
            when(accountRepository.countByCustomerId(customerId)).thenReturn(1L);
            when(accountRepository.findTopByCustomerIdOrderByAccountNumberDesc(customerId)).thenReturn(Optional.of(existingAccount));
            when(accountNumberGenerator.getUniqueNumber("020-1222-1234567-000")).thenReturn(1234567);
            when(accountNumberGenerator.getSuffix("020-1222-1234567-000")).thenReturn(0);
            when(accountNumberGenerator.build(1234567, 1)).thenReturn("020-1222-1234567-001");
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AccountResponse response = accountService.createAdditionalAccount(customerId, request);

            // Then
            assertThat(response.accountNumber()).isEqualTo("020-1222-1234567-001");
            assertThat(response.customerName()).isEqualTo("John Doe");
            assertThat(response.accountNickname()).isEqualTo("SecondAccount");
        }

        @Test
        void shouldThrowExceptionWhenAccountLimitExceeded() {
            // Given
            UUID customerId = UUID.randomUUID();
            CreateAdditionalAccountRequest request = new CreateAdditionalAccountRequest("SixthAccount");

            when(profanityClient.containsProfanity("SixthAccount")).thenReturn(false);
            when(accountRepository.countByCustomerId(customerId)).thenReturn(5L);

            // When & Then
            assertThatThrownBy(() -> accountService.createAdditionalAccount(customerId, request))
                    .isInstanceOf(AccountLimitExceededException.class);
        }

        // TODO: Test additional account with null nickname
        // TODO: Test profane nickname rejection
        // TODO: Test customer not found error
        // TODO: Test creating exactly 5 accounts
        // TODO: Test suffix increments correctly
    }
}
