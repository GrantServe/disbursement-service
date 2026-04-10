package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.*;
import com.cognizant.disbursement_service.entity.Allocation;
import com.cognizant.disbursement_service.enums.BudgetStatus;
import com.cognizant.disbursement_service.enums.ProgramStatus;
import com.cognizant.disbursement_service.exception.AllocationException;
import com.cognizant.disbursement_service.feign.BudgetServiceClient;
import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.feign.ProgramServiceClient;
import com.cognizant.disbursement_service.repository.AllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationServiceImplTest {

    @Mock private GrantServiceClient grantClient;
    @Mock private ProgramServiceClient programClient;
    @Mock private BudgetServiceClient budgetClient;
    @Mock private AllocationRepository allocationRepo;

    @InjectMocks
    private AllocationServiceImpl allocationService;

    private AllocationDto allocationDto;
    private GrantApplicationDto grantAppDto;
    private ProgramDto programDto;
    private BudgetDto budgetDto;

    @BeforeEach
    void setUp() {
        // 1. Initialize AllocationDto (Record)
        allocationDto = new AllocationDto(3L);

        // 2. Initialize GrantApplicationDto
        // Params: applicationID, researcherID, programID, title, status
        grantAppDto = new GrantApplicationDto(3L, 1001L, 101L, "Cancer Research", "APPROVED");

        // 3. Initialize ProgramDto
        // Params: programID, title, description, startDate, endDate, budget, totalApplications, status
        programDto = new ProgramDto(
                101L,
                "Health Grant",
                "Description",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(1),
                10000.0,
                10,
                ProgramStatus.ACTIVE
        );

        // 4. Initialize BudgetDto
        // Params: budgetID, allocatedAmount, spentAmount, remainingAmount, status, programId
        // Validation check: spentAmount (1000) + remainingAmount (4000) == allocatedAmount (5000)
        budgetDto = new BudgetDto(500L, 5000.0, 1000.0, 4000.0, BudgetStatus.ACTIVE, 101L);
    }

    @Test
    void testCreateAllocation_Success() throws AllocationException {
        // Arrange
        when(allocationRepo.existsByApplicationID(3L)).thenReturn(false);
        when(grantClient.getApplication(3L)).thenReturn(grantAppDto);
        when(programClient.searchProgram(101L)).thenReturn(List.of(programDto));
        when(budgetClient.getBudgetByProgram(101L)).thenReturn(budgetDto);

        // Act
        // Calculation: 5000 (budget) / 10 (apps) = 500.0 share
        String result = allocationService.createAllocation(allocationDto);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Successfully allocated 500.0"));
        verify(budgetClient, times(1)).allocateFundToResearcher(500L, 500.0);
        verify(allocationRepo, times(1)).save(any(Allocation.class));
    }

    @Test
    void testCreateAllocation_AlreadyExists_ThrowsConflict() {
        // Arrange
        when(allocationRepo.existsByApplicationID(3L)).thenReturn(true);

        // Act & Assert
        AllocationException ex = assertThrows(AllocationException.class, () ->
                allocationService.createAllocation(allocationDto)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void testCreateAllocation_NotApproved_ThrowsBadRequest() {
        // Arrange
        GrantApplicationDto pendingApp = new GrantApplicationDto(3L, 1001L, 101L, "Title", "PENDING");
        when(allocationRepo.existsByApplicationID(3L)).thenReturn(false);
        when(grantClient.getApplication(3L)).thenReturn(pendingApp);

        // Act & Assert
        AllocationException ex = assertThrows(AllocationException.class, () ->
                allocationService.createAllocation(allocationDto)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("Application is not Approved", ex.getMessage());
    }

    @Test
    void testCreateAllocation_ProgramNotFound_ThrowsNotFound() {
        // Arrange
        when(allocationRepo.existsByApplicationID(3L)).thenReturn(false);
        when(grantClient.getApplication(3L)).thenReturn(grantAppDto);
        when(programClient.searchProgram(101L)).thenReturn(List.of()); // Empty list

        // Act & Assert
        AllocationException ex = assertThrows(AllocationException.class, () ->
                allocationService.createAllocation(allocationDto)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void testCreateAllocation_ZeroCapacity_ThrowsBadRequest() {
        // Arrange
        ProgramDto fullProgram = new ProgramDto(
                101L, "Full", "Desc", LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(1), 10000.0, 0, ProgramStatus.ACTIVE
        );
        when(allocationRepo.existsByApplicationID(3L)).thenReturn(false);
        when(grantClient.getApplication(3L)).thenReturn(grantAppDto);
        when(programClient.searchProgram(101L)).thenReturn(List.of(fullProgram));

        // Act & Assert
        AllocationException ex = assertThrows(AllocationException.class, () ->
                allocationService.createAllocation(allocationDto)
        );
        assertEquals("No slots left in program", ex.getMessage());
    }
}