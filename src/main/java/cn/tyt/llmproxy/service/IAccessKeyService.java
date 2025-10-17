package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.AccessKeyInfo;
import cn.tyt.llmproxy.entity.AccessKey;

import java.util.List;

public interface IAccessKeyService {
    public boolean isValid(String keyValue);
    public boolean isBalanceSufficient(String keyValue);
    public AccessKeyInfo getAccessKeyInfo(String keyValue);
    /**
     * 为当前登录用户创建一个新的AccessKey
     * @return 创建的AccessKey实体
     */
    AccessKey createAccessKey();

    /**
     * 获取当前登录用户的所有AccessKey
     * @return AccessKey列表
     */
    List<AccessKey> getAccessKeys();

    /**
     * 删除当前登录用户自己的一个AccessKey
     * @param keyId 要删除的AccessKey的ID
     */
    String deleteMyAccessKey(Integer keyId);
}
