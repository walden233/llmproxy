package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.mapper.AccessKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AccessKeyValidationService {

    @Autowired
    private AccessKeyMapper accessKeyMapper;

    /**
     * 校验AccessKey是否有效。
     * 使用缓存来提高性能，避免每次请求都查询数据库。
     *
     * @param keyValue 待校验的key
     * @return 如果有效返回true，否则返回false
     */
    //@Cacheable(value = "api-keys", key = "#keyValue", unless = "#result == false")
    public boolean isValid(String keyValue) {
        if (keyValue == null || keyValue.trim().isEmpty()) {
            return false;
        }

        // 查询一个存在的、并且是激活状态的Key
        QueryWrapper<AccessKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("key_value", keyValue).eq("is_active", 1);

        // 使用 .exists() 方法，性能比 selectCount() 或 selectOne() 更好
        return accessKeyMapper.exists(queryWrapper);
    }
}