package com.westpac.assessment.account.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            AccountNotFoundException.class,
            CustomerNotFoundException.class
    })
    ResponseEntity<ProblemDetail> handleNotFound(
            RuntimeException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.NOT_FOUND
                );

        problem.setTitle(
                ex instanceof AccountNotFoundException 
                    ? "Account not found" 
                    : "Customer not found"
        );
        problem.setDetail(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problem);
    }

    @ExceptionHandler({
            AccountLimitExceededException.class,
            InvalidNicknameException.class
    })
    ResponseEntity<ProblemDetail> handleBadRequest(
            RuntimeException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );

        problem.setTitle("Invalid request");
        problem.setDetail(ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );

        problem.setTitle("Validation failed");

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Invalid request");

        problem.setDetail(message);

        return ResponseEntity
                .badRequest()
                .body(problem);
    }

    @ExceptionHandler(
            ProfanityServiceUnavailableException.class
    )
    ResponseEntity<ProblemDetail> handleProfanityFailure(
            ProfanityServiceUnavailableException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.SERVICE_UNAVAILABLE
                );

        problem.setTitle(
                "Service temporarily unavailable"
        );

        problem.setDetail(
                "Account creation is temporarily unavailable. "
                        + "Please try again later."
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problem);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetail> handleDatabaseFailure(
            DataAccessException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.SERVICE_UNAVAILABLE
                );

        problem.setTitle(
                "Service temporarily unavailable"
        );

        problem.setDetail(
                "The account service is temporarily unavailable."
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problem);
    }
}
