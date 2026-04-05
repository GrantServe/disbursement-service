package com.cognizant.disbursement_service.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Disbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disbursementID;

    private Double amount;
    private java.time.LocalDate date;
    private String status;


    private Long applicationID;

    @OneToOne(mappedBy = "disbursement", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Payment payment;


}