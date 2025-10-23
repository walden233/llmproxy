package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.entity.ProviderKey;
import cn.tyt.llmproxy.mapper.ProviderKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class KeySelectionService {

    @Autowired
    private ProviderKeyMapper providerKeyMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Redis Key 的前缀
    private static final String RR_COUNTER_KEY_PREFIX = "llm_proxy:provider:rr_counter:";
    private static final String CIRCUIT_KEY_PREFIX = "llm_proxy:key:circuit:";

    /**
     * 熔断默认超时时间（秒）
     * 例如，如果发生 429，熔断 60 秒
     */
    public static final long DEFAULT_CIRCUIT_TIMEOUT = 60;

    /**
     * 1. 选择一个可用的 Key
     * (轮询 + 熔断检查)
     *
     * @param providerId 提供商 ID
     * @return 选中的 ProviderKey
     */
    public ProviderKey selectAvailableKey(Integer providerId) {
        // 1. 从数据库获取所有“活跃”的 Key
        List<ProviderKey> activeKeys = providerKeyMapper.selectList(
                new LambdaQueryWrapper<ProviderKey>()
                        .eq(ProviderKey::getProviderId, providerId)
                        .eq(ProviderKey::getStatus, ProviderKey.STATUS_ACTIVE)
        );

        if (activeKeys.isEmpty()) {
            throw new RuntimeException("提供商 (ID: " + providerId + ") 已无任何[活跃]状态的 Key");
        }

        // 2. 获取 Redis 中的轮询计数器，实现原子性递增
        // key 示例: "llm:provider:rr_counter:1"
        String counterKey = RR_COUNTER_KEY_PREFIX + providerId;
        long startIndex = stringRedisTemplate.opsForValue().increment(counterKey);

        // 3. 轮询检查（最多检查 N 次，N=activeKeys.size()）
        for (int i = 0; i < activeKeys.size(); i++) {

            // 4. 计算轮询索引
            // (startIndex + i) 确保我们从当前计数器位置开始，并向后尝试所有 key
            int index = (int) ((startIndex + i) % activeKeys.size());
            ProviderKey candidateKey = activeKeys.get(index);

            // 5. 检查该 Key 是否处于“熔断”状态
            // key 示例: "llm:key:circuit:123" (123 = providerKeyId)
            String circuitKey = CIRCUIT_KEY_PREFIX + candidateKey.getId();

            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(circuitKey))) {
                // 6. 未熔断！这就是我们要的 Key
                return candidateKey;
            }
        }

        // 7. 如果循环走完，说明所有“活跃”的 Key 当前都处于“熔断”状态
        throw new RuntimeException("提供商 (ID: " + providerId + ") 所有 Key 均处于临时熔断状态，请稍后重试");
    }

    /**
     * 2. 报告 Key 调用失败（触发熔断）
     *
     * @param keyId 失败的 ProviderKey ID
     * @param timeoutSeconds 熔断时长（秒）
     */
    public void reportKeyFailure(Integer keyId, long timeoutSeconds) {
        if (keyId == null) return;

        String circuitKey = CIRCUIT_KEY_PREFIX + keyId;
        // 设置一个带 TTL 的 Key，当 Key 过期后，熔断自动解除
        stringRedisTemplate.opsForValue().set(
                circuitKey,
                "OPEN", // 值不重要，存在即代表熔断
                timeoutSeconds,
                TimeUnit.SECONDS
        );
    }

}