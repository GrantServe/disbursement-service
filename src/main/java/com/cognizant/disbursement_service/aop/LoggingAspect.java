package com.cognizant.disbursement_service.aop;

import com.cognizant.disbursement_service.feign.AuditLogClient;
import com.cognizant.disbursement_service.dto.AuditLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Autowired
    private AuditLogClient auditLogClient;

    // Define a pointcut to target all methods in your service package
    @Pointcut("execution(* com.cognizant.disbursement_service.service.*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Proceed with the actual method execution
        Object result = joinPoint.proceed();

        long timeTaken = System.currentTimeMillis() - startTime;

        log.info("Method {} executed in {} ms",
            joinPoint.getSignature().toShortString(),
            timeTaken);

        auditLogClient.addLog(
                AuditLogDTO
                        .builder()
                        .action(joinPoint.getSignature().toShortString())
                        .resource("Disbursement")
                        .build()
        );

        return result;
    }
}