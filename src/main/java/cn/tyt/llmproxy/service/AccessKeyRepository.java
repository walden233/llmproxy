package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.mapper.AccessKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
//为了解决 @Cacheable self-invocation (in effect, a method within the target object calling another method of the target object). The cache annotation will be ignored at runtime
@Service
public class AccessKeyRepository {

    @Autowired
    private AccessKeyMapper accessKeyMapper;

    @Cacheable(value = "access-keys", key = "#keyValue")
    public AccessKey findActiveKey(String keyValue) { // Renamed for clarity
        QueryWrapper<AccessKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("key_value", keyValue).eq("is_active", 1);
        return accessKeyMapper.selectOne(queryWrapper);
    }
}
