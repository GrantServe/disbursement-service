package com.cognizant.disbursement_service.repository;

import com.cognizant.disbursement_service.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {

    List<Disbursement> findByApplicationID(Long applicationID);

    List<Disbursement> findByStatus(String status);

    List<Disbursement> findByApplicationIDIn(List<Long> applicationIDs);

}

