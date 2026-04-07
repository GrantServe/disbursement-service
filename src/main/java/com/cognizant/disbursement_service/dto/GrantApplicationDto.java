package com.cognizant.disbursement_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GrantApplicationDto(

        Long applicationID,

        @NotNull(message = "Researcher ID is required")
        Long researcherID,

        @JsonProperty("programId")
        @NotNull(message = "Program ID is required")
        Long programID,

        @NotBlank(message = "Grant title is required")
        String title,

        String status
) {}