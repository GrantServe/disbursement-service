package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.AllocationDto;
import com.cognizant.disbursement_service.exception.AllocationException;

public interface IAllocationService {
    String createAllocation(AllocationDto allocationDto) throws AllocationException;

}