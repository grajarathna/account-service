package com.westpac.assessment.account.controller;

import com.westpac.assessment.account.dto.AccountResponse;
import com.westpac.assessment.account.dto.CreateAccountRequest;
import com.westpac.assessment.account.exception.AccountNotFoundException;
import com.westpac.assessment.account.exception.GlobalExceptionHandler;
import com.westpac.assessment.account.exception.InvalidNicknameException;
import com.westpac.assessment.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateAccount() throws Exception {
        // Given
        UUID customerId = UUID.randomUUID();
        AccountResponse response = new AccountResponse(customerId, "020-1222-1234567-000", "John Doe", "MySavings");
        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "customerName": "John Doe",
                                    "accountNickname": "MySavings"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("020-1222-1234567-000"))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.accountNickname").value("MySavings"));
    }

    @Test
    void shouldRejectInvalidAccountCreation() throws Exception {
        // Given
        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenThrow(new InvalidNicknameException("Account nickname is not permitted"));

        // When & Then
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "customerName": "John Doe",
                                    "accountNickname": "BadWord"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAccount() throws Exception {
        // Given
        UUID customerId = UUID.randomUUID();
        AccountResponse response = new AccountResponse(customerId, "020-1222-1234567-000", "John Doe", "MySavings");
        when(accountService.getAccount("020-1222-1234567-000")).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/accounts/020-1222-1234567-000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("020-1222-1234567-000"))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.accountNickname").value("MySavings"));
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        // Given
        when(accountService.getAccount("020-1222-9999999-000"))
                .thenThrow(new AccountNotFoundException("020-1222-9999999-000"));

        // When & Then
        mockMvc.perform(get("/api/v1/accounts/020-1222-9999999-000"))
                .andExpect(status().isNotFound());
    }

    // TODO: Test validation errors (empty name, invalid nickname length)
}
