package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.ApprovedApplicationWithDisbursementDto;
import com.cognizant.disbursement_service.dto.BudgetDto;
import com.cognizant.disbursement_service.feign.BudgetServiceClient;
import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import com.cognizant.disbursement_service.dto.DisbursementDto;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.entity.Payment;
import com.cognizant.disbursement_service.exception.DisbursementException;
import com.cognizant.disbursement_service.repository.DisbursementRepository;
import com.cognizant.disbursement_service.util.ClassUtilSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        GrantApplicationDto app = grantClient.getApplication(dto.applicationID());
        if (app == null) {
            throw new DisbursementException("Application not found", HttpStatus.NOT_FOUND);
        }
        if (!"APPROVED".equalsIgnoreCase(app.status())) {
            throw new DisbursementException("Application status must be APPROVED. Current: " + app.status(), HttpStatus.BAD_REQUEST);
        }

        BudgetDto budget = budgetClient.getBudgetByProgram(dto.programID());
        if (budget == null) {
            throw new DisbursementException("Budget not found for Program: " + dto.programID(), HttpStatus.NOT_FOUND);
        }
        if (dto.amount() > budget.remainingAmount()) {
            throw new DisbursementException("Insufficient budget. Available: " + budget.remainingAmount(), HttpStatus.BAD_REQUEST);
        }

        try {
            budgetClient.allocateFundToResearcher(budget.budgetID(), dto.amount());
        } catch (Exception e) {
            throw new DisbursementException("Failed to update Budget Service", HttpStatus.SERVICE_UNAVAILABLE);
        }

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
            return List.of();
        }
        List<Disbursement> disbursements = disbursementRepo.findByApplicationIDIn(applicationIDs);
        log.info("Found {} disbursements for Researcher ID: {}", disbursements.size(), researcherID);
        return disbursements;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovedApplicationWithDisbursementDto> getApprovedApplicationsWithDisbursementByResearcherId(Long researcherId) {
        log.info("Fetching approved applications with disbursement details for Researcher ID: {}", researcherId);

        List<ApprovedApplicationWithDisbursementDto> result = new ArrayList<>();

        try {
            // Step 1: Fetch ALL applications for the researcher from APPLICATION-SERVICE
            List<GrantApplicationDto> allApps = grantClient.fetchGrantApplications(researcherId);

            if (allApps == null || allApps.isEmpty()) {
                log.warn("No applications found for Researcher ID: {}", researcherId);
                return result;
            }

            log.info("Found {} total applications for Researcher ID: {}", allApps.size(), researcherId);

            // Step 2: Filter for ONLY APPROVED applications
            List<GrantApplicationDto> approvedApps = allApps.stream()
                    .filter(app -> "APPROVED".equalsIgnoreCase(app.status()))
                    .toList();

            log.info("Found {} approved applications for Researcher ID: {}", approvedApps.size(), researcherId);

            if (approvedApps.isEmpty()) {
                log.warn("No APPROVED applications found for Researcher ID: {}", researcherId);
                return result;
            }

            // Step 3: For each approved application, fetch corresponding disbursement and payment details
            for (GrantApplicationDto app : approvedApps) {
                ApprovedApplicationWithDisbursementDto dto = new ApprovedApplicationWithDisbursementDto();

                // Set application details
                dto.setApplicationID(app.applicationID());
                dto.setProgramID(app.programID());
                dto.setTitle(app.title());
                dto.setApplicationStatus("APPROVED");

                // Step 4: Find disbursement for this application (including payment if exists)
                Optional<Disbursement> disbursementOpt = disbursementRepo.findByApplicationIDWithPayment(app.applicationID());

                if (disbursementOpt.isPresent()) {
                    Disbursement disbursement = disbursementOpt.get();

                    // Set disbursement details
                    dto.setDisbursementID(disbursement.getDisbursementID());
                    dto.setDisbursementAmount(disbursement.getAmount());
                    dto.setDisbursementDate(disbursement.getDate());
                    dto.setDisbursementStatus(disbursement.getStatus());

                    // Set payment details if payment exists
                    if (disbursement.getPayment() != null) {
                        Payment payment = disbursement.getPayment();
                        ApprovedApplicationWithDisbursementDto.PaymentDetailsDto paymentDto =
                                new ApprovedApplicationWithDisbursementDto.PaymentDetailsDto();
                        paymentDto.setPaymentID(payment.getPaymentID());
                        paymentDto.setPaymentMethod(payment.getMethod() != null ? payment.getMethod().toString() : null);
                        paymentDto.setPaymentDate(payment.getDate());
                        paymentDto.setPaymentStatus(payment.getStatus());
                        dto.setPaymentDetails(paymentDto);
                    } else {
                        dto.setPaymentDetails(null);
                    }
                } else {
                    // No disbursement exists for this approved application
                    dto.setDisbursementID(null);
                    dto.setDisbursementAmount(null);
                    dto.setDisbursementDate(null);
                    dto.setDisbursementStatus(null);
                    dto.setPaymentDetails(null);
                    log.debug("No disbursement found for approved Application ID: {}", app.applicationID());
                }

                result.add(dto);
            }

            log.info("Successfully retrieved {} approved applications with disbursement details for Researcher ID: {}",
                    result.size(), researcherId);

        } catch (Exception e) {
            log.error("Error fetching approved applications with disbursement details for Researcher ID: {}", researcherId, e);
            throw new DisbursementException("Failed to fetch approved applications with disbursement details", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

}
