package com.cognizant.disbursement_service.feign;

import com.cognizant.disbursement_service.dto.ProgramDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "PROGRAM-SERVICE")
public interface ProgramServiceClient {
    // There is no direct "getOne" in your controller,
    // but usually, it's inferred. If not, use search:
    @GetMapping("/api/v1/programs/search")
    List<ProgramDto> searchProgram(@RequestParam("id") Long id);
}