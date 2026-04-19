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
        // DTO: disbursementID, method, amount, date
        paymentDto = new PaymentDto(50L, PaymentMethod.BANK, 1500.0, LocalDate.now());

        disbursement = new Disbursement();
        disbursement.setDisbursementID(50L);
        disbursement.setAmount(1500.0);
        disbursement.setStatus("INITIATED");

        payment = new Payment();
        payment.setPaymentID(1L);
        payment.setMethod(PaymentMethod.BANK);
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
        assertEquals("COMPLETED", disbursement.getStatus());
        verify(disbursementRepo, times(1)).save(disbursement);
        verify(paymentRepo, times(1)).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_AlreadyPaid_ThrowsConflict() {
        // Arrange
        when(disbursementRepo.findById(50L)).thenReturn(Optional.of(disbursement));
        // Simulate that a payment record already exists
        when(paymentRepo.findByDisbursement_DisbursementID(50L)).thenReturn(Optional.of(payment));

        // Act & Assert
        PaymentException ex = assertThrows(PaymentException.class, () ->
                paymentService.processPayment(paymentDto)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void testGetPaymentsByResearcher_Success() {
        // Arrange
        Long researcherID = 101L;
        GrantApplicationDto app = new GrantApplicationDto(3L, researcherID, 5L, "Title", "APPROVED");

        when(grantClient.fetchGrantApplications(researcherID)).thenReturn(List.of(app));
        when(paymentRepo.findByDisbursement_ApplicationIDIn(List.of(3L))).thenReturn(List.of(payment));

        // Act
        List<PaymentDto> results = paymentService.getPaymentsByResearcher(researcherID);

        // Assert
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(grantClient).fetchGrantApplications(researcherID);
        verify(paymentRepo).findByDisbursement_ApplicationIDIn(anyList());
    }

    @Test
    void testGetPaymentsByResearcher_NoApplications_ReturnsEmpty() {
        // Arrange
        when(grantClient.fetchGrantApplications(101L)).thenReturn(Collections.emptyList());

        // Act
        List<PaymentDto> results = paymentService.getPaymentsByResearcher(101L);

        // Assert
        assertTrue(results.isEmpty());
        verify(paymentRepo, never()).findByDisbursement_ApplicationIDIn(anyList());
    }

    @Test
    void testGetPaymentByDisbursement_NotFound_ThrowsException() {
        // Arrange
        when(paymentRepo.findByDisbursement_DisbursementID(50L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentException.class, () -> paymentService.getPaymentByDisbursement(50L));
    }
}