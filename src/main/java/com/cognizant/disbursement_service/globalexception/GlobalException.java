package com.cognizant.disbursement_service.globalexception;

import com.cognizant.disbursement_service.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.cts.grantserve.security.shared.exception.BaseSecurityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@ControllerAdvice
@Slf4j
public class GlobalException extends BaseSecurityExceptionHandler {

    @ExceptionHandler(DisbursementException.class)
    public ResponseEntity<Object> disbursementExceptionHandler(DisbursementException d) {
        return buildResponse(d.getHttpStatus(),d.getMessage());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Object> paymentExceptionHandler(PaymentException p) {
        return buildResponse(p.getHttpStatus(),p.getMessage());
    }


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



    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "An unexpected error occurred");
        body.put("error", ex.getMessage());

        log.error("Unexpected Error: ", ex);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
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

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "The requested endpoint does not exist. Please check your URL and HTTP method.");
        body.put("path", ex.getResourcePath());

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND); // Now it returns 404
    }
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<Object> handleFeignException(feign.FeignException e) {
        log.error("Feign Client Error: Status {}, Message {}", e.status(), e.getMessage());

        HttpStatus status = HttpStatus.resolve(e.status());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        String message = (status == HttpStatus.NOT_FOUND)
                ? "Clear Error: Invalid Request - Resource not found in dependency service."
                : "Clear Error: Communication with the dependency service failed.";
        return buildResponse(status, message);
    }

}