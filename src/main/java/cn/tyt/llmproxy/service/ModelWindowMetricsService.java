package cn.tyt.llmproxy.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service
public class ModelWindowMetricsService {

    private static final String MODEL_REQ_PREFIX = "llm_proxy:model:window:req:";
    private static final String MODEL_FAIL_PREFIX = "llm_proxy:model:window:fail:";
    private static final int WINDOW_MINUTES = 5;
    private static final Duration KEY_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;

    public ModelWindowMetricsService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void recordRequest(Integer modelId, boolean success) {
        if (modelId == null) {
            return;
        }
        long minuteBucket = Instant.now().getEpochSecond() / 60;
        String reqKey = MODEL_REQ_PREFIX + modelId;
        String field = Long.toString(minuteBucket);
        stringRedisTemplate.opsForHash().increment(reqKey, field, 1);
        stringRedisTemplate.expire(reqKey, KEY_TTL);
        if (!success) {
            String failKey = MODEL_FAIL_PREFIX + modelId;
            stringRedisTemplate.opsForHash().increment(failKey, field, 1);
            stringRedisTemplate.expire(failKey, KEY_TTL);
        }
    }

    public WindowStats getWindowStats(Integer modelId) {
        if (modelId == null) {
            return new WindowStats(0L, 0L);
        }
        long currentMinute = Instant.now().getEpochSecond() / 60;
        long fromMinute = currentMinute - WINDOW_MINUTES + 1L;
        long requestCount = sumRange(stringRedisTemplate.opsForHash().entries(MODEL_REQ_PREFIX + modelId), fromMinute, currentMinute);
        long failureCount = sumRange(stringRedisTemplate.opsForHash().entries(MODEL_FAIL_PREFIX + modelId), fromMinute, currentMinute);
        return new WindowStats(requestCount, failureCount);
    }

    public long getWindowRequestCount(Integer modelId) {
        return getWindowStats(modelId).getRequestCount();
    }

    private long sumRange(Map<Object, Object> entries, long fromMinute, long toMinute) {
        if (entries == null || entries.isEmpty()) {
            return 0L;
        }
        long sum = 0L;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long minute = toLong(entry.getKey());
            if (minute == null || minute < fromMinute || minute > toMinute) {
                continue;
            }
            Long count = toLong(entry.getValue());
            if (count != null) {
                sum += count;
            }
        }
        return sum;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static class WindowStats {
        private final long requestCount;
        private final long failureCount;

        public WindowStats(long requestCount, long failureCount) {
            this.requestCount = requestCount;
            this.failureCount = failureCount;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public double getFailureRate() {
            if (requestCount <= 0) {
                return 0D;
            }
            return failureCount * 1.0D / requestCount;
        }
    }
}
