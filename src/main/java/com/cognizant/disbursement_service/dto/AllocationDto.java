package com.cognizant.disbursement_service.dto;

import jakarta.validation.constraints.NotNull;

public record AllocationDto(
        @NotNull(message = "Application ID must be provided")
        Long applicationID
) {
}
