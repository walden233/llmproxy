package cn.tyt.llmproxy.aspect;

import cn.tyt.llmproxy.common.annotation.LogExecution;
import cn.tyt.llmproxy.common.utils.SensitiveDataMasker;
import cn.tyt.llmproxy.common.utils.TraceIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private static final int MAX_BODY_LENGTH = 400;
    private static final int MAX_USER_AGENT_LENGTH = 160;

    private final ObjectMapper objectMapper;

    // 定义切点：拦截所有Controller方法
    @Pointcut("execution(* cn.tyt.llmproxy.controller..*.*(..))")
    public void controllerMethods() {}

    // 定义切点：拦截带有@LogExecution注解的方法
    @Pointcut("@annotation(cn.tyt.llmproxy.common.annotation.LogExecution)")
    public void logExecutionMethods() {}

    /**
     * 环绕通知：记录Controller方法的执行情况
     */
    @Around("controllerMethods()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = resolveRequest();
        TraceIdUtil.ensureTraceId();
        return logAround(joinPoint, request, true);
    }

    /**
     * 环绕通知：仅记录显式标注 @LogExecution 的 Service 方法，避免和 Controller 重复。
     */
    @Around("logExecutionMethods() && !within(cn.tyt.llmproxy.controller..*)")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        TraceIdUtil.ensureTraceId();
        return logAround(joinPoint, null, false);
    }

    private Object logAround(ProceedingJoinPoint joinPoint, HttpServletRequest request, boolean includeHttpMeta) throws Throwable {
        String traceId = TraceIdUtil.getTraceId();
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            logRequestStart(traceId, methodName, joinPoint.getArgs(), request, includeHttpMeta);
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            exception = ex;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestEnd(traceId, methodName, result, exception, duration);
        }
    }

    private void logRequestStart(String traceId, String methodName, Object[] args,
                                 HttpServletRequest request, boolean includeHttpMeta) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("[%s] 调用开始: %s", traceId, methodName));

        if (includeHttpMeta && request != null) {
            logMessage.append(String.format(" - %s %s", request.getMethod(), request.getRequestURI()));

            String clientIp = getClientIpAddress(request);
            if (clientIp != null) {
                logMessage.append(String.format(" - IP: %s", clientIp));
            }

            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                logMessage.append(String.format(" - UA: %s", truncate(userAgent, MAX_USER_AGENT_LENGTH)));
            }
        }

        if (args != null && args.length > 0) {
            logMessage.append(String.format(" - 参数: %s", formatArgs(args)));
        }

        log.info(logMessage.toString());
    }

    private void logRequestEnd(String traceId, String methodName, Object result, Throwable exception, long duration) {
        if (exception == null) {
            log.info("[{}] 调用完成: {} - 耗时: {}ms - 返回值: {}",
                    traceId, methodName, duration, formatResult(result));
        } else {
            log.error("[{}] 调用失败: {} - 耗时: {}ms - 异常: {}",
                    traceId, methodName, duration, exception.getClass().getName(), exception);
        }
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        try {
            Object[] filteredArgs = Arrays.stream(args)
                    .map(SensitiveDataMasker::mask)
                    .toArray();
            return truncate(objectMapper.writeValueAsString(filteredArgs), MAX_BODY_LENGTH);
        } catch (Exception e) {
            return truncate(Arrays.toString(args), MAX_BODY_LENGTH);
        }
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        try {
            Object filteredResult = SensitiveDataMasker.mask(result);
            String resultStr = objectMapper.writeValueAsString(filteredResult);
            return truncate(resultStr, MAX_BODY_LENGTH);
        } catch (Exception e) {
            return truncate(result.toString(), MAX_BODY_LENGTH);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private HttpServletRequest resolveRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest();
        } catch (Exception e) {
            log.warn("获取 HttpServletRequest 失败: {}", e.getMessage());
            return null;
        }
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
