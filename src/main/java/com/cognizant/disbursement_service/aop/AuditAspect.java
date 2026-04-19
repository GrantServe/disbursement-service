package com.cognizant.disbursement_service.aop;

import com.cognizant.disbursement_service.feign.AuditLogClient;
import com.cognizant.disbursement_service.dto.AuditLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Autowired
    AuditLogClient auditLogClient;

    @Around("execution(* com.cognizant.disbursement_service.controller.*.*(..))")
    public Object auditAction(ProceedingJoinPoint joinPoint) throws Throwable {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null) ? auth.getName() : "Anonymous";

        log.info("USER: {} is attempting to call: {}", user, joinPoint.getSignature().getName());

        auditLogClient.addLog(
                AuditLogDTO
                        .builder()
                        .action(joinPoint.getSignature().toShortString())
                        .resource("Disbursement")
                        .build()
        );

        return joinPoint.proceed();
    }
}