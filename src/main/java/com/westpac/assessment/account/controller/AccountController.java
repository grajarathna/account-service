package com.westpac.assessment.account.controller;

import com.westpac.assessment.account.dto.AccountResponse;
import com.westpac.assessment.account.dto.CreateAccountRequest;
import com.westpac.assessment.account.dto.CreateAdditionalAccountRequest;
import com.westpac.assessment.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest accountRequest){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accountService.createAccount(accountRequest));
    }

    @GetMapping("/{accountNumber}")
    ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber){
        return ResponseEntity
                .ok(accountService.getAccount(accountNumber));
    }

    @PostMapping("/{customerId}")
    public ResponseEntity<AccountResponse> createAdditionalAccount(@PathVariable UUID customerId,
                                                                   @Valid @RequestBody CreateAdditionalAccountRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accountService.createAdditionalAccount(
                        customerId,
                        request
                ));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccounts(@PathVariable UUID customerId) {
        return ResponseEntity.ok(accountService.getAccounts(customerId));
    }
}
