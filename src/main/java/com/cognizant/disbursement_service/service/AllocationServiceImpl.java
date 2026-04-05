package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.BudgetDto;
import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import com.cognizant.disbursement_service.dto.ProgramDto;
import com.cognizant.disbursement_service.feign.BudgetServiceClient;
import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.feign.ProgramServiceClient;
import com.cognizant.disbursement_service.dto.AllocationDto;
import com.cognizant.disbursement_service.entity.Allocation;
import com.cognizant.disbursement_service.exception.AllocationException;
import com.cognizant.disbursement_service.repository.AllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AllocationServiceImpl implements IAllocationService {

    @Autowired private GrantServiceClient grantClient;
    @Autowired private ProgramServiceClient programClient;
    @Autowired private BudgetServiceClient budgetClient;
    @Autowired private AllocationRepository allocationRepo;

    @Override
    @Transactional
    public String createAllocation(AllocationDto allocationDto) throws AllocationException {
        Long appId = allocationDto.applicationID();

        // 1. Validate Application Status (from Grant Service)
        GrantApplicationDto app = grantClient.getApplication(appId);
        if (app == null || !"APPROVED".equalsIgnoreCase(app.status())) {
            throw new AllocationException("Application is not Approved", HttpStatus.BAD_REQUEST);
        }

        // 2. Get Program Capacity (from Program Service)
        Long pId = app.programID();
        List<ProgramDto> programs = programClient.searchProgram(pId);
        if (programs.isEmpty()) {
            throw new AllocationException("Program not found", HttpStatus.NOT_FOUND);
        }
        ProgramDto program = programs.get(0);

        if (program.count() <= 0) {
            throw new AllocationException("No slots left in program", HttpStatus.BAD_REQUEST);
        }

        // 3. Get Budget Details (from Budget Service)
        BudgetDto budget = budgetClient.getBudgetByProgram(pId);
        if (budget == null) {
            throw new AllocationException("Budget not found", HttpStatus.NOT_FOUND);
        }

        // 4. Calculate Share
        Double shareAmount = budget.allocatedAmount() / program.count();

        // 5. Update Budget Spent (Remote Call to Budget Service)
        // This triggers your PatchMapping in BudgetController
        budgetClient.updateBudgetSpent(budget.budgetID(), shareAmount);

        // 6. Save Allocation Locally (Finance Service Database)
        Allocation allocation = new Allocation();
        allocation.setApplicationID(appId);
        allocation.setProgramID(pId);
        allocation.setTotalAwardedAmount(shareAmount);
        allocation.setDisbursedAmount(0.0);
        allocation.setRemainingBalance(shareAmount);
        allocation.setStatus("ALLOCATED");

        allocationRepo.save(allocation);

        return "Successfully allocated " + shareAmount + " to Application ID: " + appId;
    }
}

