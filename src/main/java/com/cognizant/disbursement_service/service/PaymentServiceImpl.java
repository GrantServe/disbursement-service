package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.PaymentDto;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.entity.Payment;
import com.cognizant.disbursement_service.enums.PaymentMethod;
import com.cognizant.disbursement_service.exception.PaymentException;
import com.cognizant.disbursement_service.repository.DisbursementRepository;
import com.cognizant.disbursement_service.repository.PaymentRepository;
import com.cognizant.disbursement_service.util.ClassUtilSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentServiceImpl implements IPaymentService {

    @Autowired
    private PaymentRepository paymentRepo;

    @Autowired
    private DisbursementRepository disbursementRepo;

    @Override
    @Transactional
    public Payment processPayment(PaymentDto dto) {
        log.info("Processing payment request for Disbursement ID: {}", dto.disbursementID());

        // 1. Check if Disbursement exists
        Disbursement disbursement = disbursementRepo.findById(dto.disbursementID())
                .orElseThrow(() -> {
                    log.error("Payment Process Failed: Disbursement ID {} not found", dto.disbursementID());
                    return new PaymentException("Disbursement not found with ID: " + dto.disbursementID(), HttpStatus.NOT_FOUND);
                });

        // 2. Check if payment already exists (Strict check)
        paymentRepo.findByDisbursement_DisbursementID(dto.disbursementID())
                .ifPresent(p -> {
                    log.warn("Payment Conflict: A payment already exists for Disbursement ID {}", dto.disbursementID());
                    throw new PaymentException("Payment already processed for this disbursement", HttpStatus.CONFLICT);
                });

        // 3. Map DTO to Entity
        Payment payment = ClassUtilSeparator.PaymentUtil(dto);
        payment.setDisbursement(disbursement);

        // 4. Update Disbursement Status to COMPLETED
        disbursement.setStatus("COMPLETED");
        disbursementRepo.save(disbursement);
        log.info("Disbursement status updated to COMPLETED for ID: {}", dto.disbursementID());

        // 5. Save the final Payment record
        Payment savedPayment = paymentRepo.save(payment);
        log.info("Payment record successfully created with ID: {}", savedPayment.getPaymentID());

        return savedPayment;
    }

    @Override
    public List<Payment> getPaymentsByMethod(PaymentMethod method) {
        log.info("Fetching all payments with method: {}", method);
        return paymentRepo.findByMethod(method);
    }

    @Override
    public Payment getPaymentByDisbursement(Long disbursementID) {
        log.info("Fetching payment record for Disbursement ID: {}", disbursementID);
        return paymentRepo.findByDisbursement_DisbursementID(disbursementID)
                .orElseThrow(() -> {
                    log.warn("No payment record found for Disbursement ID: {}", disbursementID);
                    return new PaymentException("No payment found for Disbursement ID: " + disbursementID, HttpStatus.NOT_FOUND);
                });
    }

    @Override
    public List<Payment> getAllPayments() {
        log.info("Fetching all payment records from the database");
        return paymentRepo.findAll();
    }

    @Override
    @Transactional
    public void deletePayment(Long paymentID) {
        log.info("Initiating deletion for Payment ID: {}", paymentID);
        if (!paymentRepo.existsById(paymentID)) {
            log.error("Delete Action Failed: Payment ID {} does not exist", paymentID);
            throw new PaymentException("Payment ID not found", HttpStatus.NOT_FOUND);
        }
        paymentRepo.deleteById(paymentID);
        log.info("Payment ID: {} successfully deleted", paymentID);
    }

//    @Override
//    public List<PaymentDto> getPaymentsByResearcher(Long researcherID) {
//        log.info("Retrieving payment history for Researcher ID: {}", researcherID);
//
//        List<Payment> payments = paymentRepo.findByDisbursement_Application_Researcher_ResearcherID(researcherID);
//
//        if (payments.isEmpty()) {
//            log.info("Researcher ID {} has no recorded payments", researcherID);
//        } else {
//            log.info("Found {} payment records for Researcher ID {}", payments.size(), researcherID);
//        }
//
//        return payments.stream()
//                .map(payment -> new PaymentDto(
//                        payment.getDisbursement().getDisbursementID(),
//                        payment.getMethod(),
//                        payment.getDisbursement().getAmount()
//                ))
//                .collect(Collectors.toList());
//    }

}