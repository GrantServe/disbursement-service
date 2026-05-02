package com.cognizant.disbursement_service.controller;

import com.cognizant.disbursement_service.dto.PaymentDto;
import com.cognizant.disbursement_service.entity.Payment;
import com.cognizant.disbursement_service.enums.PaymentMethod;
import com.cognizant.disbursement_service.service.IPaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@Slf4j
public class PaymentController {

    @Autowired
    private IPaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<String> process(@Valid @RequestBody PaymentDto dto) {
        log.info("REST Request: Processing payment for Disbursement ID: {}", dto.disbursementID());

        Payment payment = paymentService.processPayment(dto);

        log.info("REST Success: Payment processed with System ID: {}", payment.getPaymentID());
        return new ResponseEntity<>("Payment processed successfully", HttpStatus.CREATED);
    }

    @GetMapping("/disbursement/{disbursementId}")
    public ResponseEntity<Payment> getByDisbursement(@PathVariable Long disbursementId) {
        log.info("REST Request: Fetching payment details for Disbursement ID: {}", disbursementId);

        Payment payment = paymentService.getPaymentByDisbursement(disbursementId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/method/{method}")
    public ResponseEntity<List<Payment>> getByMethod(@PathVariable PaymentMethod method) {
        log.info("REST Request: Filtering payments by Method: {}", method);

        List<Payment> payments = paymentService.getPaymentsByMethod(method);
        log.info("REST Response: Found {} payments for method: {}", payments.size(), method);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/researcher/{id}")
    public ResponseEntity<List<PaymentDto>> getResearcherPayments(@PathVariable Long id) {
        log.info("REST Request: Retrieving payment history for Researcher ID: {}", id);

        List<PaymentDto> history = paymentService.getPaymentsByResearcher(id);
        log.info("REST Response: Returning {} records for Researcher ID: {}", history.size(), id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Payment>> getAll() {
        log.info("REST Request: Fetching all global payment records");

        List<Payment> allPayments = paymentService.getAllPayments();
        return ResponseEntity.ok(allPayments);
    }
}