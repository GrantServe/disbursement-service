package com.cognizant.disbursement_service.repository;

import com.cognizant.disbursement_service.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    Optional<Allocation> findByApplicationID(Long applicationId);
    boolean existsByApplicationID(Long applicationID);
}