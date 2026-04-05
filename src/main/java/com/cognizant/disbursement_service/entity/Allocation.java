package com.cognizant.disbursement_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long allocationID;

    private Long programID;

    private Long applicationID;

    private Long budgetID;

    private Double totalAwardedAmount;

    private Double disbursedAmount = 0.0;

    private Double remainingBalance;

    private String status;

}