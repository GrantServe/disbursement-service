package com.cognizant.disbursement_service.dto;

import com.cognizant.disbursement_service.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentDto(
        @NotNull(message = "Disbursement ID is required")
        Long disbursementID,

        @NotNull(message = "Payment method is required")
        PaymentMethod method,

        Double amount
) {
}