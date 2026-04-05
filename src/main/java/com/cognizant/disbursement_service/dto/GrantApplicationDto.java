package com.cognizant.disbursement_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GrantApplicationDto(
        @NotNull(message = "Researcher ID is required")
        Long researcherID,

        @NotNull(message = "Program ID is required")
        Long programID,

        @NotBlank(message = "Grant title is required")
        String title,

        String status
) {}