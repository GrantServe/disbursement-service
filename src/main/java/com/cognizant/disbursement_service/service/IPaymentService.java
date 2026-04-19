package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.PaymentDto;
import com.cognizant.disbursement_service.entity.Payment;
import com.cognizant.disbursement_service.enums.PaymentMethod;

import java.util.List;

public interface IPaymentService {
    Payment processPayment(PaymentDto dto);
    List<Payment> getPaymentsByMethod(PaymentMethod method);
    Payment getPaymentByDisbursement(Long disbursementID);
    List<Payment> getAllPayments();
    List<PaymentDto> getPaymentsByResearcher(Long researcherID);

}