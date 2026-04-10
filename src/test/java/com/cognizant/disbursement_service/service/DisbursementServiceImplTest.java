package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.DisbursementDto;
import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import com.cognizant.disbursement_service.entity.Allocation;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.exception.DisbursementException;
import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.repository.AllocationRepository;
import com.cognizant.disbursement_service.repository.DisbursementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisbursementServiceImplTest {

    @Mock private DisbursementRepository disbursementRepo;
    @Mock private AllocationRepository allocationRepo;
    @Mock private GrantServiceClient grantClient;

    @InjectMocks
    private DisbursementServiceImpl disbursementService;

    private DisbursementDto disbursementDto;
    private Allocation allocation;
    private GrantApplicationDto grantAppDto;

    @BeforeEach
    void setUp() {
        // Updated to match your Record: applicationID, programID, amount
        disbursementDto = new DisbursementDto(3L, 101L, 1000.0);

        // Local Allocation entity setup
        allocation = new Allocation();
        allocation.setApplicationID(3L);
        allocation.setProgramID(101L);
        allocation.setTotalAwardedAmount(5000.0);
        allocation.setDisbursedAmount(500.0);
        allocation.setRemainingBalance(4500.0);
        allocation.setStatus("ALLOCATED");

        // Remote Grant Service DTO setup
        // applicationID, researcherID, programID, title, status
        grantAppDto = new GrantApplicationDto(3L, 1001L, 101L, "Research Project", "APPROVED");
    }

    @Test
    void testInitiateDisbursement_Success() {
        // Arrange
        when(allocationRepo.findByApplicationID(3L)).thenReturn(Optional.of(allocation));
        when(grantClient.getApplication(3L)).thenReturn(grantAppDto);

        Disbursement mockSaved = new Disbursement();
        mockSaved.setDisbursementID(1L);
        mockSaved.setApplicationID(3L);
        when(disbursementRepo.save(any(Disbursement.class))).thenReturn(mockSaved);

        // Act
        Disbursement result = disbursementService.initiateDisbursement(disbursementDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getDisbursementID());

        // Logic Check: 4500 (remaining) - 1000 (requested) = 3500
        assertEquals(3500.0, allocation.getRemainingBalance());
        assertEquals(1500.0, allocation.getDisbursedAmount());

        verify(allocationRepo, times(1)).save(allocation);
        verify(disbursementRepo, times(1)).save(any(Disbursement.class));
    }

    @Test
    void testInitiateDisbursement_ExhaustsBalance() {
        // Arrange: Set remaining balance exactly equal to disbursement amount
        allocation.setTotalAwardedAmount(1000.0);
        allocation.setDisbursedAmount(0.0);
        allocation.setRemainingBalance(1000.0);

        when(allocationRepo.findByApplicationID(3L)).thenReturn(Optional.of(allocation));
        when(grantClient.getApplication(3L)).thenReturn(grantAppDto);
        when(disbursementRepo.save(any(Disbursement.class))).thenReturn(new Disbursement());

        // Act
        disbursementService.initiateDisbursement(disbursementDto);

        // Assert
        assertEquals(0.0, allocation.getRemainingBalance());
        assertEquals("EXHAUSTED", allocation.getStatus());
    }

    @Test
    void testTrackByApplication() {
        // Act
        disbursementService.trackByApplication(3L);

        // Assert
        verify(disbursementRepo, times(1)).findByApplicationID(3L);
    }
}