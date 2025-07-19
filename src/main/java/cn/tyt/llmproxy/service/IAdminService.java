package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.request.AdminLoginRequest;
import cn.tyt.llmproxy.dto.request.AdminRegisterRequest;
import cn.tyt.llmproxy.dto.response.AdminLoginResponse;
import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.entity.Admin;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;


public interface IAdminService extends UserDetailsService { // 继承 UserDetailsService 以便 Spring Security 使用
    Admin register(AdminRegisterRequest request);
    AdminLoginResponse login(AdminLoginRequest request);
    /**
     * 为指定管理员创建一个新的AccessKey
     * @return 创建的AccessKey实体
     */
    AccessKey createAccessKey();

    /**
     * 获取指定管理员的所有AccessKey
     * @return AccessKey列表
     */
    List<AccessKey> getAccessKeys();

    void deleteMyAccessKey(Integer keyId);
}