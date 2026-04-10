package com.cognizant.disbursement_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
		"com.cognizant.disbursement_service",
		"com.cts.grantserve.security"
})
@EnableFeignClients
public class DisbursementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DisbursementServiceApplication.class, args);
	}

}
