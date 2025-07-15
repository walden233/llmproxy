package cn.tyt.llmproxy.aspect;

import cn.tyt.llmproxy.common.annotation.LogExecution;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Autowired
    private ObjectMapper objectMapper;

    // 定义切点：拦截所有Controller方法
    @Pointcut("execution(* cn.tyt.llmproxy.controller..*.*(..))")
    public void controllerMethods() {}

    // 定义切点：拦截所有Service方法
    @Pointcut("execution(* cn.tyt.llmproxy.service..*.*(..))")
    public void serviceMethods() {}

    // 定义切点：拦截带有@LogExecution注解的方法
    @Pointcut("@annotation(cn.tyt.llmproxy.common.annotation.LogExecution)")
    public void logExecutionMethods() {}

    /**
     * 环绕通知：记录Controller方法的执行情况
     */
    @Around("controllerMethods()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        // 获取HTTP请求信息
        HttpServletRequest request = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
            }
        } catch (Exception e) {
            log.warn("无法获取HttpServletRequest: {}", e.getMessage());
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            // 记录请求开始
            logRequestStart(traceId, methodName, joinPoint.getArgs(), request);

            // 执行目标方法
            result = joinPoint.proceed();

            return result;
        } catch (Throwable ex) {
            exception = ex;
            throw ex;
        } finally {
            // 记录请求结束
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logRequestEnd(traceId, methodName, result, exception, duration);
        }
    }

    /**
     * 环绕通知：记录Service方法的执行情况
     */
//    @Around("serviceMethods()")
//    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
//        String traceId = UUID.randomUUID().toString().substring(0, 8);
//        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
//
//        long startTime = System.currentTimeMillis();
//        Object result = null;
//        Throwable exception = null;
//
//        try {
//            log.debug("[{}] Service方法开始执行: {}", traceId, methodName);
//
//            // 执行目标方法
//            result = joinPoint.proceed();
//
//            return result;
//        } catch (Throwable ex) {
//            exception = ex;
//            log.error("[{}] Service方法执行异常: {} - {}", traceId, methodName, ex.getMessage());
//            throw ex;
//        } finally {
//            long endTime = System.currentTimeMillis();
//            long duration = endTime - startTime;
//
//            if (exception == null) {
//                log.debug("[{}] Service方法执行完成: {} - 耗时: {}ms", traceId, methodName, duration);
//            } else {
//                log.error("[{}] Service方法执行失败: {} - 耗时: {}ms", traceId, methodName, duration);
//            }
//        }
//    }

    /**
     * 环绕通知：记录带有@LogExecution注解的方法
     */
    @Around("logExecutionMethods() && @annotation(logExecution)")
    public Object logAnnotatedExecution(ProceedingJoinPoint joinPoint, LogExecution logExecution) throws Throwable {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            if (logExecution.logArgs()) {
                log.info("[{}] 方法开始执行: {} - 参数: {}", traceId, methodName,
                        formatArgs(joinPoint.getArgs()));
            } else {
                log.info("[{}] 方法开始执行: {}", traceId, methodName);
            }

            result = joinPoint.proceed();

            return result;
        } catch (Throwable ex) {
            exception = ex;
            log.error("[{}] 方法执行异常: {} - {}", traceId, methodName, ex.getMessage());
            throw ex;
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (exception == null) {
                if (logExecution.logResult()) {
                    log.info("[{}] 方法执行完成: {} - 耗时: {}ms - 返回值: {}",
                            traceId, methodName, duration, formatResult(result));
                } else {
                    log.info("[{}] 方法执行完成: {} - 耗时: {}ms", traceId, methodName, duration);
                }
            } else {
                log.error("[{}] 方法执行失败: {} - 耗时: {}ms", traceId, methodName, duration);
            }
        }
    }

//    /**
//     * 异常通知：记录所有异常
//     */
//    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", throwing = "ex")
//    public void logException(JoinPoint joinPoint, Throwable ex) {
//        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
//        log.error("方法执行异常: {} - 异常类型: {} - 异常信息: {}",
//                methodName, ex.getClass().getSimpleName(), ex.getMessage());
//    }

    private void logRequestStart(String traceId, String methodName, Object[] args, HttpServletRequest request) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("[%s] 请求开始: %s", traceId, methodName));

        if (request != null) {
            logMessage.append(String.format(" - %s %s", request.getMethod(), request.getRequestURI()));

            // 记录客户端IP
            String clientIp = getClientIpAddress(request);
            if (clientIp != null) {
                logMessage.append(String.format(" - IP: %s", clientIp));
            }

            // 记录User-Agent
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                logMessage.append(String.format(" - UA: %s", userAgent));
            }
        }

        // 记录请求参数（敏感信息过滤）
        if (args != null && args.length > 0) {
            logMessage.append(String.format(" - 参数: %s", formatArgs(args)));
        }

        log.info(logMessage.toString());
    }

    private void logRequestEnd(String traceId, String methodName, Object result, Throwable exception, long duration) {
        if (exception == null) {
            log.info("[{}] 请求完成: {} - 耗时: {}ms - 返回值: {}",
                    traceId, methodName, duration, formatResult(result));
        } else {
            log.error("[{}] 请求失败: {} - 耗时: {}ms - 异常: {}",
                    traceId, methodName, duration, exception.getMessage());
        }
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        try {
            // 过滤敏感信息
            Object[] filteredArgs = Arrays.stream(args)
                    .map(this::filterSensitiveInfo)
                    .toArray();
            return objectMapper.writeValueAsString(filteredArgs);
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        try {
            Object filteredResult = filterSensitiveInfo(result);
            String resultStr = objectMapper.writeValueAsString(filteredResult);
            // 限制日志长度
            return resultStr.length() > 1000 ? resultStr.substring(0, 1000) + "..." : resultStr;
        } catch (Exception e) {
            return result.toString();
        }
    }

    private Object filterSensitiveInfo(Object obj) {
        if (obj == null) {
            return null;
        }

        String objStr = obj.toString();
         //简单的敏感信息过滤，实际项目中可以更完善
        if (objStr.toLowerCase().contains("password") ||
                objStr.toLowerCase().contains("apikey")) {
            return "***FILTERED***";
        }

        return obj;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}