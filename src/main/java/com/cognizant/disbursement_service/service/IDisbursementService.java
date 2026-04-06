package com.cognizant.disbursement_service.service;

import com.cognizant.disbursement_service.dto.DisbursementDto;
import com.cognizant.disbursement_service.entity.Disbursement;
import java.util.List;

public interface IDisbursementService {


    Disbursement initiateDisbursement(DisbursementDto dto);

    List<Disbursement> trackByApplication(Long appId);


    List<Disbursement> trackByStatus(String status);


    void deleteDisbursement(Long id);
}
