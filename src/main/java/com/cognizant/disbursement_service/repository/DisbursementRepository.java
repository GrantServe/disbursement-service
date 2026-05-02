package com.cognizant.disbursement_service.repository;

import com.cognizant.disbursement_service.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {

    List<Disbursement> findByApplicationID(Long applicationID);

    List<Disbursement> findByStatus(String status);

    List<Disbursement> findByApplicationIDIn(List<Long> applicationIDs);

    @Query("SELECT d FROM Disbursement d LEFT JOIN FETCH d.payment WHERE d.applicationID = :applicationID")
    Optional<Disbursement> findByApplicationIDWithPayment(@Param("applicationID") Long applicationID);
}

