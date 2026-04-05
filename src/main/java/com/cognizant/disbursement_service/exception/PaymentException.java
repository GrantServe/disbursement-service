package com.cognizant.disbursement_service.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class PaymentException extends RuntimeException {
    private HttpStatus httpStatus;

    public PaymentException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}