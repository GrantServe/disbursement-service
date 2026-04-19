package com.cognizant.disbursement_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogDTO {

    @NotBlank(message = "Action cannot be blank")
    private String action;

    @NotBlank(message = "Resource cannot be blank")
    private String resource;

}