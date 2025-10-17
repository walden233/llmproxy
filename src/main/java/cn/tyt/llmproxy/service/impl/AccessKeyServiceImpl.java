package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.dto.AccessKeyInfo;
import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.entity.User;
import cn.tyt.llmproxy.mapper.AccessKeyMapper;
import cn.tyt.llmproxy.mapper.UserMapper;
import cn.tyt.llmproxy.service.AccessKeyRepository;
import cn.tyt.llmproxy.service.IAccessKeyService;
import cn.tyt.llmproxy.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccessKeyServiceImpl implements IAccessKeyService {

    @Autowired
    private AccessKeyMapper accessKeyMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private IUserService userService;
    @Autowired
    private AccessKeyRepository accessKeyRepository;
    /**
     * 校验AccessKey是否有效。
     *
     * @param keyValue 待校验的key
     * @return 如果有效返回true，否则返回false
     */
    //@Cacheable(value = "api-keys", key = "#keyValue", unless = "#result == false")
    @Override
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

    @Override
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
    //在这添加缓存
//    @Cacheable(value = "access-key", key = "#keyValue")
//    public AccessKey getAccessKey(String keyValue){
//        QueryWrapper<AccessKey> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("key_value", keyValue).eq("is_active", 1);
//        return accessKeyMapper.selectOne(queryWrapper);
//    }
    @Override
    public AccessKeyInfo getAccessKeyInfo(String keyValue){
        AccessKeyInfo keyInfo = new AccessKeyInfo();
        keyInfo.setKeyValue(keyValue);
        AccessKey accessKey = accessKeyRepository.findActiveKey(keyValue);
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

    @Override
    public AccessKey createAccessKey() {
        User currentUser = userService.getCurrentUser();
        String key = "ak-" + UUID.randomUUID().toString().replace("-", "");
        AccessKey newAccessKey = new AccessKey();
        newAccessKey.setKeyValue(key);
        newAccessKey.setUserId(currentUser.getId());
        newAccessKey.setIsActive(true);
        newAccessKey.setCreatedAt(LocalDateTime.now());
        accessKeyMapper.insert(newAccessKey);
        return newAccessKey;
    }

    @Override
    public List<AccessKey> getAccessKeys() {
        User currentUser = userService.getCurrentUser();
        return accessKeyMapper.selectList(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getUserId, currentUser.getId()));
    }

    @Override
    @Transactional
    @CacheEvict(value = "access-keys", key = "#result")
    public String deleteMyAccessKey(Integer keyId) {
        User currentUser = userService.getCurrentUser();
        AccessKey accessKeyToDelete = accessKeyMapper.selectById(keyId);
        if (accessKeyToDelete == null || !accessKeyToDelete.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Access Key not found or you don't have permission to delete it.");
        }
        accessKeyMapper.deleteById(keyId);
        return accessKeyToDelete.getKeyValue();
    }

}