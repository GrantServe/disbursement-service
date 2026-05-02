package com.cognizant.disbursement_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedApplicationWithDisbursementDto {

    private Long applicationID;
    private Long programID;
    private String title;
    private String applicationStatus;

    // Disbursement Details
    private Long disbursementID;
    private Double disbursementAmount;
    private LocalDate disbursementDate;
    private String disbursementStatus;

    // Payment Details (nullable if no payment exists)
    @JsonProperty("payment")
    private PaymentDetailsDto paymentDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetailsDto {
        private Long paymentID;
        private String paymentMethod;
        private LocalDate paymentDate;
        private String paymentStatus;
    }
}
