package com.cognizant.disbursement_service.globalexception;

import com.cognizant.disbursement_service.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@ControllerAdvice
@Slf4j
public class GlobalException {



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        log.error("Validation failed: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }



    @ExceptionHandler(DisbursementException.class)
    public ResponseEntity<String> disbursementExceptionHandler(DisbursementException d) {
        return ResponseEntity.status(d.getHttpStatus()).body(d.getMessage());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<String> paymentExceptionHandler(PaymentException p) {
        return ResponseEntity.status(p.getHttpStatus()).body(p.getMessage());
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "An unexpected error occurred");
        body.put("error", ex.getMessage()); // This helps you debug in Postman!

        log.error("Unexpected Error: ", ex);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex) {
        String errorMessage = ex.getMostSpecificCause().getMessage();
        String userFriendlyMessage = "Database error: A unique record already exists.";

        // Check for User Email Duplicate
        if (errorMessage.contains("users") || errorMessage.contains("email")) {
            userFriendlyMessage = "The email address is already in use. Please use a different one.";
        }
        // Check for Allocation Duplicate (Your new module)
        else if (errorMessage.contains("allocation")) {
            userFriendlyMessage = "Error: This application already has funds allocated.";
        }

        log.error("Data Integrity Violation: {}", ex.getMessage());

        // Returning simple string as requested
        return new ResponseEntity<>(userFriendlyMessage, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<String> handlePaymentMethodEnumError(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        String errorMsg = ex.getMessage();

        // Only trigger if the error is about the PaymentMethod enum
        if (errorMsg != null && errorMsg.contains("com.cts.grantserve.enums.PaymentMethod")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid payment method. Only BANK or WALLET are accepted.");
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Please check your request format.");
    }

    @ExceptionHandler(AllocationException.class)
    public ResponseEntity<String> handleAllocationException(AllocationException ex) {
        // Returns just the message as a String with the specific HTTP Status
        return new ResponseEntity<>(ex.getMessage(), ex.getHttpStatus());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "The requested endpoint does not exist. Please check your URL and HTTP method.");
        body.put("path", ex.getResourcePath());

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND); // Now it returns 404
    }

}