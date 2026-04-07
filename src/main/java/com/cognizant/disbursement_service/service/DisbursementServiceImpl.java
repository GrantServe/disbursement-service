package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.feign.GrantServiceClient;
import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import com.cognizant.disbursement_service.dto.DisbursementDto;
import com.cognizant.disbursement_service.entity.Allocation;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.exception.DisbursementException;
import com.cognizant.disbursement_service.repository.AllocationRepository;
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
    private AllocationRepository allocationRepo;

    @Autowired
    private GrantServiceClient grantClient; // Feign Client to talk to Grant Service

    @Override
    @Transactional
    public Disbursement initiateDisbursement(DisbursementDto dto) {
        log.info("Service: Initiating disbursement for Application ID: {} Amount: {}",
                dto.applicationID(), dto.amount());

        // 1. Fetch Allocation from Local DB (The researcher's specific "wallet")
        Allocation allocation = allocationRepo.findByApplicationID(dto.applicationID())
                .orElseThrow(() -> new DisbursementException(
                        "Funds have not been allocated yet. Allocation must be initiated first.",
                        HttpStatus.BAD_REQUEST));

        // 2. Validate against the Researcher's Remaining Balance
        if (dto.amount() > allocation.getRemainingBalance()) {
            throw new DisbursementException(
                    "Insufficient allocated funds. Remaining balance: " + allocation.getRemainingBalance(),
                    HttpStatus.BAD_REQUEST);
        }

        // 3. Verify Application exists in the Remote Grant Service
        GrantApplicationDto app = grantClient.getApplication(dto.applicationID());
        if (app == null) {
            throw new DisbursementException("Application not found in Grant Service", HttpStatus.NOT_FOUND);
        }

        // 4. Update Local Allocation Financials
        Double currentDisbursed = (allocation.getDisbursedAmount() != null) ? allocation.getDisbursedAmount() : 0.0;
        Double newDisbursedTotal = currentDisbursed + dto.amount();

        allocation.setDisbursedAmount(newDisbursedTotal);
        allocation.setRemainingBalance(allocation.getTotalAwardedAmount() - newDisbursedTotal);

        if (allocation.getRemainingBalance() <= 0) {
            allocation.setStatus("EXHAUSTED");
        }
        allocationRepo.save(allocation);

        // 5. Create and Save Disbursement Entity
        Disbursement disbursement = ClassUtilSeparator.DisbursementUtil(dto);
        disbursement.setApplicationID(dto.applicationID()); // Store ID as Long// Or use dto.status() if coming from request

        Disbursement saved = disbursementRepo.save(disbursement);

        log.info("Service Success: Disbursement ID {} created. New Balance: {}",
                saved.getDisbursementID(), allocation.getRemainingBalance());

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
}
