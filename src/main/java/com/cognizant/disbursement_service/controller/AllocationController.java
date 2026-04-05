package com.cognizant.disbursement_service.controller;

import com.cognizant.disbursement_service.dto.AllocationDto;
import com.cognizant.disbursement_service.service.IAllocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/allocate")
public class AllocationController {

    @Autowired
    private IAllocationService iAllocationService;

    @PostMapping("/initiate")
    public ResponseEntity<String> initiateAllocation(@Valid @RequestBody AllocationDto allocationDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(iAllocationService.createAllocation(allocationDto));
    }
}