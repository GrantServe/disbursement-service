package com.cognizant.disbursement_service.controller;

import com.cognizant.disbursement_service.dto.DisbursementDto;
import com.cognizant.disbursement_service.entity.Disbursement;
import com.cognizant.disbursement_service.service.IDisbursementService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/disbursements")
@CrossOrigin(origins = "*")
@Slf4j
public class DisbursementController {

    @Autowired
    private IDisbursementService disbursementService;

    @PostMapping("/initiate")
    public ResponseEntity<String> initiate(@Valid @RequestBody DisbursementDto dto) {
        log.info("REST Request: Initiating new disbursement for App ID: {}", dto.applicationID());

        Disbursement saved = disbursementService.initiateDisbursement(dto);

        return new ResponseEntity<>("Disbursement created with ID: " + saved.getDisbursementID(), HttpStatus.CREATED);
    }

    @GetMapping("/application/{appId}")
    public ResponseEntity<List<Disbursement>> getByApplication(@PathVariable Long appId) {
        log.info("REST Request: Tracking for App ID: {}", appId);
        List<Disbursement> results = disbursementService.trackByApplication(appId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Disbursement>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(disbursementService.trackByStatus(status));
    }

    @DeleteMapping("/delete/{id}") // Simplified path
    public ResponseEntity<String> delete(@PathVariable Long id) {
        disbursementService.deleteDisbursement(id);
        return ResponseEntity.ok("Disbursement " + id + " deleted successfully");
    }
}