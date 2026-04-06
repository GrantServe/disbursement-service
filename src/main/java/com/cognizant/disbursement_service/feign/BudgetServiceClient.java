package com.cognizant.disbursement_service.feign;

import com.cognizant.disbursement_service.dto.BudgetDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "budget-service")
public interface BudgetServiceClient {
    // Matches @GetMapping("/program/{programId}") in BudgetController
    @GetMapping("/api/v1/budgets/program/{programId}")
    BudgetDto getBudgetByProgram(@PathVariable("programId") Long programId);

    // Matches @PatchMapping("/{budgetId}") in BudgetController
    @PatchMapping("/api/v1/budgets/{budgetId}")
    ResponseEntity<String> allocateFundToResearcher(
            @PathVariable("budgetId") Long budgetId,
            @RequestParam("allocatedAmount") Double allocatedAmount
    );
}
