package com.cognizant.disbursement_service.feign;

import com.cognizant.disbursement_service.dto.GrantApplicationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "APPLICATION-SERVICE")
public interface GrantServiceClient {

    @GetMapping("/GrantApplication/getApplication/{id}")
    GrantApplicationDto getApplication(@PathVariable("id") long id);

    @GetMapping("/GrantApplication/FetchGrantApplication/{id}")
    List<GrantApplicationDto> fetchGrantApplications(@PathVariable("id") Long researcherId);
}
