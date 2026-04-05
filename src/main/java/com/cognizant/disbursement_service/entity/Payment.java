package com.cognizant.disbursement_service.entity;

import com.cognizant.disbursement_service.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentID;

    @OneToOne
    @JoinColumn(name = "disbursementID")
    @JsonBackReference
    private Disbursement disbursement;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private java.time.LocalDate date;
    private String status;

}