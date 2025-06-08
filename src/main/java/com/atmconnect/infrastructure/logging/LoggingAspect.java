package com.atmconnect.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    
    @Pointcut("execution(* com.atmconnect.application.usecases.*.*(..))")
    public void useCaseMethods() {}
    
    @Pointcut("execution(* com.atmconnect.infrastructure.bluetooth.*.*(..))")
    public void bluetoothMethods() {}
    
    @Pointcut("execution(* com.atmconnect.infrastructure.security.*.*(..))")
    public void securityMethods() {}
    
    @Around("useCaseMethods()")
    public Object logUseCaseExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.info("Executing use case: {}.{}", className, methodName);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("Use case {}.{} completed successfully in {}ms", 
                className, methodName, executionTime);
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.error("Use case {}.{} failed after {}ms: {}", 
                className, methodName, executionTime, e.getMessage());
            
            throw e;
        }
    }
    
    @Around("bluetoothMethods()")
    public Object logBluetoothExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        
        log.debug("Bluetooth operation: {}", methodName);
        
        try {
            Object result = joinPoint.proceed();
            log.debug("Bluetooth operation {} completed", methodName);
            return result;
        } catch (Exception e) {
            log.error("Bluetooth operation {} failed: {}", methodName, e.getMessage());
            throw e;
        }
    }
    
    @Around("securityMethods()")
    public Object logSecurityExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        
        // Do not log sensitive security operations in detail
        log.debug("Security operation: {}", methodName);
        
        try {
            Object result = joinPoint.proceed();
            log.debug("Security operation {} completed", methodName);
            return result;
        } catch (Exception e) {
            log.warn("Security operation {} failed", methodName);
            throw e;
        }
    }
    
    @AfterThrowing(pointcut = "execution(* com.atmconnect..*(..))", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.error("Exception in {}.{}: {}", className, methodName, exception.getMessage());
    }
}