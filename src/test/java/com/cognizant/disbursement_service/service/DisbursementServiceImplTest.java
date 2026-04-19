package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.BudgetDto;
import com.cognizant.disbursement_service.dto.DisbursementDto;
import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.enums.BudgetStatus;
import com.cognizant.disbursement_service.exception.DisbursementException;
import com.cognizant.disbursement_service.feign.BudgetServiceClient;
import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.repository.DisbursementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisbursementServiceImplTest {

    @Mock private DisbursementRepository disbursementRepo;
    @Mock private BudgetServiceClient budgetClient;
    @Mock private GrantServiceClient grantClient;

    @InjectMocks
    private DisbursementServiceImpl disbursementService;

    private DisbursementDto disbursementDto;
    private GrantApplicationDto grantAppDto;
    private BudgetDto budgetDto;

    @BeforeEach
    void setUp() {
        // Record: applicationID, programID, amount
        disbursementDto = new DisbursementDto(3L, 101L, 1000.0);

        // Grant DTO: applicationID, researcherID, programID, title, status
        grantAppDto = new GrantApplicationDto(3L, 1001L, 101L, "Research Grant", "APPROVED");

        // Budget DTO: budgetID, allocatedAmount, spentAmount, remainingAmount, status, programId
        budgetDto = new BudgetDto(500L, 5000.0, 1000.0, 4000.0, BudgetStatus.ACTIVE, 101L);
    }

    @Test
    void testInitiateDisbursement_Success() {
        // Arrange
        when(grantClient.getApplication(3L)).thenReturn(grantAppDto);
        when(budgetClient.getBudgetByProgram(101L)).thenReturn(budgetDto);

        Disbursement mockSaved = new Disbursement();
        mockSaved.setDisbursementID(1L);
        when(disbursementRepo.save(any(Disbursement.class))).thenReturn(mockSaved);

        // Act
        Disbursement result = disbursementService.initiateDisbursement(disbursementDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getDisbursementID());

        // Verify cross-service calls
        verify(grantClient).getApplication(3L);
        verify(budgetClient).getBudgetByProgram(101L);
        verify(budgetClient).allocateFundToResearcher(500L, 1000.0);
        verify(disbursementRepo).save(any(Disbursement.class));
    }

    @Test
    void testInitiateDisbursement_ApplicationNotApproved_ThrowsException() {
        // Arrange
        GrantApplicationDto pendingApp = new GrantApplicationDto(3L, 1001L, 101L, "Title", "PENDING");
        when(grantClient.getApplication(3L)).thenReturn(pendingApp);

        // Act & Assert
        DisbursementException ex = assertThrows(DisbursementException.class, () ->
                disbursementService.initiateDisbursement(disbursementDto)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("Application status must be APPROVED"));

        // Ensure we stop before calling the Budget Service
        verify(budgetClient, never()).getBudgetByProgram(anyLong());
    }

    @Test
    void testInitiateDisbursement_InsufficientBudget_ThrowsException() {
        // Arrange
        BudgetDto lowBudget = new BudgetDto(500L, 5000.0, 4500.0, 500.0, BudgetStatus.ACTIVE, 101L);
        // Request is 1000.0, but only 500.0 is left

        when(grantClient.getApplication(3L)).thenReturn(grantAppDto);
        when(budgetClient.getBudgetByProgram(101L)).thenReturn(lowBudget);

        // Act & Assert
        DisbursementException ex = assertThrows(DisbursementException.class, () ->
                disbursementService.initiateDisbursement(disbursementDto)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("Insufficient budget"));
    }

    @Test
    void testInitiateDisbursement_BudgetUpdateFails_ThrowsException() {
        // Arrange
        when(grantClient.getApplication(3L)).thenReturn(grantAppDto);
        when(budgetClient.getBudgetByProgram(101L)).thenReturn(budgetDto);

        // Simulate Feign error (e.g., Timeout or Network failure)
        when(budgetClient.allocateFundToResearcher(anyLong(), anyDouble()))
                .thenThrow(new RuntimeException("Connection Refused"));

        // Act & Assert
        DisbursementException ex = assertThrows(DisbursementException.class, () ->
                disbursementService.initiateDisbursement(disbursementDto)
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus() );
    }

    @Test
    void testDeleteDisbursement_Success() {
        // Arrange
        when(disbursementRepo.existsById(1L)).thenReturn(true);

        // Act
        disbursementService.deleteDisbursement(1L);

        // Assert
        verify(disbursementRepo, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteDisbursement_NotFound_ThrowsException() {
        // Arrange
        when(disbursementRepo.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(DisbursementException.class, () -> disbursementService.deleteDisbursement(1L));
    }
}