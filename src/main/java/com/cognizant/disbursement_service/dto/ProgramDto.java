package com.cognizant.disbursement_service.dto;

import com.cognizant.disbursement_service.enums.ProgramStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProgramDto(
        Long programID,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Budget is required")
        Double budget,

        @NotNull
        Integer count,

        @NotNull(message = "Status is required")
        ProgramStatus status
) {
    public ProgramDto {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (budget != null && budget < 0) {
            throw new IllegalArgumentException("Budget must be a valid positive value");
        }
        if (budget != null && status == ProgramStatus.DRAFT && budget != 0.0) {
            throw new IllegalArgumentException("Budget must be set to 0.0 for " + status + " programs");
        }
    }
}