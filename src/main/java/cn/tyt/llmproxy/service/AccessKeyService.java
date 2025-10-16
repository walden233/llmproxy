package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.AccessKeyInfo;
import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.entity.User;
import cn.tyt.llmproxy.mapper.AccessKeyMapper;
import cn.tyt.llmproxy.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessKeyService {

    @Autowired
    private AccessKeyMapper accessKeyMapper;
    @Autowired
    private UserMapper userMapper;

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

    public boolean isBalanceSufficient(String keyValue) {
        if (keyValue == null || keyValue.trim().isEmpty()) {
            return false;
        }

        // 查询一个存在的、并且是激活状态的Key
        QueryWrapper<AccessKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("key_value", keyValue).eq("is_active", 1);
        AccessKey accessKey = accessKeyMapper.selectOne(queryWrapper);
        User user = userMapper.selectById(accessKey.getUserId());
        return user.getBalance().doubleValue() > 0;
    }

    public AccessKeyInfo getAccessKeyInfo(String keyValue){
        AccessKeyInfo keyInfo = new AccessKeyInfo();
        keyInfo.setKeyValue(keyValue);
        QueryWrapper<AccessKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("key_value", keyValue).eq("is_active", 1);
        AccessKey accessKey = accessKeyMapper.selectOne(queryWrapper);
        if(accessKey==null){
            keyInfo.setValid(false);
            return keyInfo;
        }
        else keyInfo.setValid(true);

        User user = userMapper.selectById(accessKey.getUserId());
        keyInfo.setUserId(user.getId());
        if(user.getBalance().doubleValue()<=0){
            keyInfo.setBalanceSufficient(false);
            return keyInfo;
        }
        else keyInfo.setBalanceSufficient(true);
        return keyInfo;
    }


}