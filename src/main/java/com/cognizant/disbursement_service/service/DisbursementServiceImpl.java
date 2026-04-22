package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.BudgetDto;
import com.cognizant.disbursement_service.feign.BudgetServiceClient;
import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import com.cognizant.disbursement_service.dto.DisbursementDto;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.exception.DisbursementException;
import com.cognizant.disbursement_service.repository.DisbursementRepository;
import com.cognizant.disbursement_service.util.ClassUtilSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DisbursementServiceImpl implements IDisbursementService {

    @Autowired
    private DisbursementRepository disbursementRepo;

    @Autowired
    private BudgetServiceClient budgetClient;

    @Autowired
    private GrantServiceClient grantClient;

    @Override
    @Transactional
    public Disbursement initiateDisbursement(DisbursementDto dto) {
        log.info("Initiating disbursement process for App ID: {}", dto.applicationID());

        // 1. Verify Approval Status
        GrantApplicationDto app = grantClient.getApplication(dto.applicationID());
        if (app == null) {
            throw new DisbursementException("Application not found", HttpStatus.NOT_FOUND);
        }
        if (!"APPROVED".equalsIgnoreCase(app.status())) {
            throw new DisbursementException("Application status must be APPROVED. Current: " + app.status(), HttpStatus.BAD_REQUEST);
        }

        // 2. Check Budget Sufficiency
        BudgetDto budget = budgetClient.getBudgetByProgram(dto.programID());
        if (budget == null) {
            throw new DisbursementException("Budget not found for Program: " + dto.programID(), HttpStatus.NOT_FOUND);
        }
        if (dto.amount() > budget.remainingAmount()) {
            throw new DisbursementException("Insufficient budget. Available: " + budget.remainingAmount(), HttpStatus.BAD_REQUEST);
        }

        // 3. Update Remote Budget
        try {
            budgetClient.allocateFundToResearcher(budget.budgetID(), dto.amount());
        } catch (Exception e) {
            throw new DisbursementException("Failed to update Budget Service", HttpStatus.SERVICE_UNAVAILABLE);
        }

        // 4. Save Local Record
        Disbursement disbursement = ClassUtilSeparator.DisbursementUtil(dto);

        Disbursement saved = disbursementRepo.save(disbursement);
        log.info("Successfully created Disbursement ID: {}", saved.getDisbursementID());

        return saved;
    }

    @Override
    public List<Disbursement> trackByApplication(Long appId) {
        log.info("Fetching disbursements from local DB for Application ID: {}", appId);
        return disbursementRepo.findByApplicationID(appId);
    }

    @Override
    public List<Disbursement> trackByStatus(String status) {
        log.info("Filtering disbursements by status: {}", status);
        return disbursementRepo.findByStatus(status);
    }

    @Override
    @Transactional
    public void deleteDisbursement(Long id) {
        log.info("Attempting to delete disbursement record ID: {}", id);

        if (!disbursementRepo.existsById(id)) {
            log.error("Delete Failed: Disbursement ID {} does not exist", id);
            throw new DisbursementException("Cannot delete: Disbursement ID not found", HttpStatus.NOT_FOUND);
        }

        disbursementRepo.deleteById(id);
        log.info("Successfully deleted disbursement record ID: {}", id);
    }


       @Override
       public List<Disbursement> trackByResearcher(Long researcherID) {
           log.info("Tracking disbursements for Researcher ID: {}", researcherID);
           List<GrantApplicationDto> applications = grantClient.fetchGrantApplications(researcherID);
           List<Long> applicationIDs = applications.stream()
               .map(GrantApplicationDto::applicationID)
               .toList();
           if (applicationIDs.isEmpty()) {
               log.warn("No applications found for Researcher ID: {}", researcherID);
               return List.of(); // Return empty list
           }
           List<Disbursement> disbursements = disbursementRepo.findByApplicationIDIn(applicationIDs);
           log.info("Found {} disbursements for Researcher ID: {}", disbursements.size(), researcherID);
           return disbursements;
       }
}
