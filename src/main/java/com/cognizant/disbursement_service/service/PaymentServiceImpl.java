package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.PaymentDto;
import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.entity.Payment;
import com.cognizant.disbursement_service.enums.PaymentMethod;
import com.cognizant.disbursement_service.exception.PaymentException;
import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.repository.DisbursementRepository;
import com.cognizant.disbursement_service.repository.PaymentRepository;
import com.cognizant.disbursement_service.util.ClassUtilSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentServiceImpl implements IPaymentService {

    @Autowired
    private PaymentRepository paymentRepo;

    @Autowired
    private GrantServiceClient grantClient;

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

        // 2. Check if payment already exists
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
    public List<PaymentDto> getPaymentsByResearcher(Long researcherID) {
        log.info("Fetching payment history for Researcher ID: {}", researcherID);

        // Step 1: Call Grant Service to get all applications for this researcher
        List<GrantApplicationDto> applications = grantClient.fetchGrantApplications(researcherID);

        if (applications == null || applications.isEmpty()) {
            log.info("No applications found for Researcher ID: {}", researcherID);
            return Collections.emptyList();
        }

        // Step 2: Extract the Application IDs from the list of DTOs
        List<Long> appIds = applications.stream()
                .map(GrantApplicationDto::applicationID)
                .collect(Collectors.toList());

        // Step 3: Fetch payments from the LOCAL database using these IDs
        List<Payment> payments = paymentRepo.findByDisbursement_ApplicationIDIn(appIds);

        // Step 4: Map the Entity to DTO for the response
        return payments.stream()
                .map(payment -> new PaymentDto(
                        payment.getPaymentID(),
                        payment.getMethod(),
                        payment.getDisbursement().getAmount(),
                        payment.getDate()
                ))
                .collect(Collectors.toList());
    }

}