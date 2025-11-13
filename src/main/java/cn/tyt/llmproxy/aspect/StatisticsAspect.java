package cn.tyt.llmproxy.aspect;

import cn.tyt.llmproxy.common.utils.TraceIdUtil;
import cn.tyt.llmproxy.context.ModelUsageContext;
import cn.tyt.llmproxy.service.IStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class StatisticsAspect {

    private final IStatisticsService statisticsService;

    /**
     * 定义切点：拦截所有 IProxyService 接口的实现类中的 public 方法。
     */
    @Pointcut("execution(public * cn.tyt.llmproxy.service.ILangchainProxyService.*(..))")
    public void proxyServiceMethods() {}


    @Around("proxyServiceMethods() && args(request, userId, accessKeyId, isAsync)")
    public Object recordStatistics(ProceedingJoinPoint joinPoint, Object request, Integer userId, Integer accessKeyId, Boolean isAsync) throws Throwable {
        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            log.error("代理方法 {} 执行失败，traceId={}", joinPoint.getSignature().getName(), TraceIdUtil.getTraceId());
            throw ex;
        } finally {
            writeStatistics(userId, accessKeyId, isAsync, success, joinPoint);
        }
    }

    private void writeStatistics(Integer userId, Integer accessKeyId, Boolean isAsync, boolean success, ProceedingJoinPoint joinPoint) {
        ModelUsageContext.ModelUsageInfo usageInfo = ModelUsageContext.get();
        try {
            if (usageInfo == null) {
                log.warn("方法 {} 执行完毕但 ModelUsageContext 为空，traceId={}", joinPoint.getSignature().getName(), TraceIdUtil.getTraceId());
                return;
            }
            log.info("记录模型用量: modelId={}, success={}, traceId={}", usageInfo.getModelId(), success, TraceIdUtil.getTraceId());
            statisticsService.recordUsageMysql(usageInfo.getModelId(), usageInfo.getModelIdentifier(), success);
            if (!success) {
                statisticsService.recordFailMongo(userId, accessKeyId, usageInfo.getModelId(), LocalDateTime.now(), isAsync);
            }
        } catch (Exception e) {
            log.error("记录模型统计失败, traceId={}", TraceIdUtil.getTraceId(), e);
        } finally {
            ModelUsageContext.clear();
        }
    }
}
