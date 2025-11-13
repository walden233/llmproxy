package cn.tyt.llmproxy.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 简单的 TraceId 辅助工具，负责生成/读取/清理 TraceId.
 */
public final class TraceIdUtil {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String[] HEADER_CANDIDATES = {"X-Trace-Id", "X-Request-Id", "Trace-Id"};

    private TraceIdUtil() {
    }

    /**
     * 从请求头获取 TraceId，不存在则自动生成，并写入 MDC.
     */
    public static String initTraceId(HttpServletRequest request) {
        String headerTraceId = request == null ? null : resolveFromHeaders(request);
        return initTraceId(headerTraceId);
    }

    /**
     * 在未设置时生成新的 TraceId；若已有则保持不变.
     */
    public static String ensureTraceId() {
        String current = MDC.get(TRACE_ID_KEY);
        return StringUtils.hasText(current) ? current : initTraceId((String) null);
    }

    /**
     * 从自定义来源初始化 TraceId.
     */
    public static String initTraceId(String sourceTraceId) {
        String traceId = StringUtils.hasText(sourceTraceId) ? sourceTraceId : UUID.randomUUID().toString().replace("-", "");
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static boolean hasTraceId() {
        return StringUtils.hasText(getTraceId());
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }

    private static String resolveFromHeaders(HttpServletRequest request) {
        for (String header : HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
