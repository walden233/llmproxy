package cn.tyt.llmproxy.context;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用于在单个请求线程中传递模型使用信息的上下文。
 */
public final class ModelUsageContext {

    // 内部类，用于存储模型信息
    @Getter
    @AllArgsConstructor
    public static class ModelUsageInfo {
        private final Integer modelId;
        private final String modelIdentifier;
    }

    private static final ThreadLocal<ModelUsageInfo> contextHolder = new ThreadLocal<>();

    /**
     * 设置当前线程的模型使用信息。
     * @param modelId 使用的模型ID
     * @param modelIdentifier 使用的模型标识符
     */
    public static void set(Integer modelId, String modelIdentifier) {
        contextHolder.set(new ModelUsageInfo(modelId, modelIdentifier));
    }

    /**
     * 获取当前线程的模型使用信息。
     * @return ModelUsageInfo 或 null
     */
    public static ModelUsageInfo get() {
        return contextHolder.get();
    }

    /**
     * 清除当前线程的上下文信息，防止内存泄漏。
     */
    public static void clear() {
        contextHolder.remove();
    }
}
