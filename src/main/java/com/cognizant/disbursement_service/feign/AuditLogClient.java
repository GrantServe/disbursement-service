package com.cognizant.disbursement_service.feign;

import com.cognizant.disbursement_service.dto.AuditLogDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "AUDIT-LOG-SERVICE")
public interface AuditLogClient {

    @PostMapping("/api/v1/audit-log/add")
    void addLog(AuditLogDTO logDTO);

}
