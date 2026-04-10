package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import com.cognizant.disbursement_service.dto.PaymentDto;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.entity.Payment;
import com.cognizant.disbursement_service.enums.PaymentMethod;
import com.cognizant.disbursement_service.exception.PaymentException;
import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.repository.DisbursementRepository;
import com.cognizant.disbursement_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepo;
    @Mock private DisbursementRepository disbursementRepo;
    @Mock private GrantServiceClient grantClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentDto paymentDto;
    private Disbursement disbursement;
    private Payment payment;

    @BeforeEach
    void setUp() {
        // Record: disbursementID, method, amount, date
        paymentDto = new PaymentDto(50L, PaymentMethod.BANK, 1500.0, LocalDate.now());

        disbursement = new Disbursement();
        disbursement.setDisbursementID(50L);
        disbursement.setAmount(1500.0);
        disbursement.setStatus("INITIATED");
        disbursement.setApplicationID(3L);

        payment = new Payment();
        payment.setPaymentID(1L);
        payment.setMethod(PaymentMethod.BANK);
        payment.setDate(LocalDate.now());
        payment.setDisbursement(disbursement);
    }

    @Test
    void testProcessPayment_Success() {
        // Arrange
        when(disbursementRepo.findById(50L)).thenReturn(Optional.of(disbursement));
        when(paymentRepo.findByDisbursement_DisbursementID(50L)).thenReturn(Optional.empty());
        when(paymentRepo.save(any(Payment.class))).thenReturn(payment);

        // Act
        Payment result = paymentService.processPayment(paymentDto);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", disbursement.getStatus()); // Verify side effect
        verify(disbursementRepo, times(1)).save(disbursement);
        verify(paymentRepo, times(1)).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_DisbursementNotFound_ThrowsException() {
        // Arrange
        when(disbursementRepo.findById(50L)).thenReturn(Optional.empty());

        // Act & Assert
        PaymentException ex = assertThrows(PaymentException.class, () ->
                paymentService.processPayment(paymentDto)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void testProcessPayment_AlreadyProcessed_ThrowsConflict() {
        // Arrange
        when(disbursementRepo.findById(50L)).thenReturn(Optional.of(disbursement));
        when(paymentRepo.findByDisbursement_DisbursementID(50L)).thenReturn(Optional.of(payment));

        // Act & Assert
        PaymentException ex = assertThrows(PaymentException.class, () ->
                paymentService.processPayment(paymentDto)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals("Payment already processed for this disbursement", ex.getMessage());
    }

    @Test
    void testGetPaymentsByResearcher_Success() {
        // Arrange
        Long researcherID = 1001L;
        GrantApplicationDto appDto = new GrantApplicationDto(3L, researcherID, 101L, "Title", "APPROVED");

        when(grantClient.fetchGrantApplications(researcherID)).thenReturn(List.of(appDto));
        when(paymentRepo.findByDisbursement_ApplicationIDIn(List.of(3L))).thenReturn(List.of(payment));

        // Act
        List<PaymentDto> results = paymentService.getPaymentsByResearcher(researcherID);

        // Assert
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).disbursementID()); // Based on your mapping in Step 4
        verify(grantClient, times(1)).fetchGrantApplications(researcherID);
    }

    @Test
    void testGetPaymentsByResearcher_NoApps_ReturnsEmptyList() {
        // Arrange
        when(grantClient.fetchGrantApplications(1001L)).thenReturn(Collections.emptyList());

        // Act
        List<PaymentDto> results = paymentService.getPaymentsByResearcher(1001L);

        // Assert
        assertTrue(results.isEmpty());
        verify(paymentRepo, never()).findByDisbursement_ApplicationIDIn(any());
    }

    @Test
    void testDeletePayment_Success() {
        // Arrange
        when(paymentRepo.existsById(1L)).thenReturn(true);

        // Act
        paymentService.deletePayment(1L);

        // Assert
        verify(paymentRepo, times(1)).deleteById(1L);
    }

    @Test
    void testDeletePayment_NotFound_ThrowsException() {
        // Arrange
        when(paymentRepo.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(PaymentException.class, () -> paymentService.deletePayment(1L));
    }
}