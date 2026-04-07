package com.cognizant.disbursement_service.dto;

import com.cognizant.disbursement_service.enums.BudgetStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BudgetDto(

        Long budgetID,

        @NotNull(message = "Allocated amount is required")
        Double allocatedAmount,

        @NotNull(message = "Spent amount amount is required")
        Double spentAmount,

        @NotNull(message = "Remaining amount is required")
        Double remainingAmount,

        @NotBlank(message = "Status is required")
        BudgetStatus status,

        @NotNull(message = "Program ID is required")
        Long programId
) {
    public BudgetDto {
        if (allocatedAmount != null && allocatedAmount < 0) {
            throw new IllegalArgumentException("Allocated amount must be >= 0");
        }
        if (spentAmount != null && remainingAmount != null && allocatedAmount != null) {
            if (spentAmount + remainingAmount != (double)allocatedAmount) {
                throw new IllegalArgumentException("The sum of spent and remaining must equal allocated amount");
            }
        }
    }
}