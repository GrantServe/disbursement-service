package com.cognizant.disbursement_service.util;

import com.cognizant.disbursement_service.dto.*;
import com.cognizant.disbursement_service.entity.*;
import lombok.Data;

import java.time.LocalDate;


@Data
public class ClassUtilSeparator {


    public static Disbursement DisbursementUtil(DisbursementDto disbursementDto){
        Disbursement disbursement = new Disbursement();
        disbursement.setAmount(disbursementDto.amount());
        disbursement.setDate(LocalDate.now());
        disbursement.setStatus("PENDING");
        return disbursement;
    }
    public static Payment PaymentUtil(PaymentDto paymentDto){
        Payment payment=new Payment();
        payment.setMethod(paymentDto.method());
        payment.setStatus("SUCCESS");
        payment.setDate(LocalDate.now());
        return payment;
    }

}
