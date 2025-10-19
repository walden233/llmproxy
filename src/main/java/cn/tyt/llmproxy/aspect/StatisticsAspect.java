package cn.tyt.llmproxy.aspect;


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
     * 这样可以精确地定位到 chat, generateImage 等代理方法。
     * 这种方式比直接指定实现类更具扩展性。
     */
    @Pointcut("execution(public * cn.tyt.llmproxy.service.ILangchainProxyService.*(..))")
    public void proxyServiceMethods() {}


    @Around("proxyServiceMethods() && args(request, userId, accessKeyId)")
    public Object recordStatistics(ProceedingJoinPoint joinPoint,Object request, Integer userId, Integer accessKeyId) throws Throwable {
        boolean success = false;
        try {
            // 执行目标方法（例如 ProxyServiceImpl.chat()）
            Object result = joinPoint.proceed();
            success = true; // 如果没有异常，则标记为成功
            return result;
        } catch (Throwable ex) {
            // 如果有异常，success 保持 false
            log.error("代理方法 {} 执行失败", joinPoint.getSignature().getName());
            throw ex; // 必须重新抛出异常，否则调用方无法感知错误
        } finally {
            // 从上下文中获取模型信息
            ModelUsageContext.ModelUsageInfo usageInfo = ModelUsageContext.get();


            if (usageInfo != null) {
                // 如果上下文中有信息，则记录用量
                log.info("记录模型用量: modelId={}, success={}", usageInfo.getModelId(), success);
                statisticsService.recordUsageMysql(usageInfo.getModelId(), usageInfo.getModelIdentifier(), success);
                if(!success)
                    statisticsService.recordFailMongo(userId, accessKeyId, usageInfo.getModelId(), LocalDateTime.now());
                ModelUsageContext.clear();
            } else {
                // 正常情况下不应该发生，除非有代理方法没有设置上下文
                log.warn("方法 {} 执行完毕，但 ModelUsageContext 为空，无法记录统计数据。", joinPoint.getSignature().getName());
            }
        }
    }
}