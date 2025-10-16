package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.request.UserChangePasswordRequest;
import cn.tyt.llmproxy.dto.request.UserLoginRequest;
import cn.tyt.llmproxy.dto.request.UserRegisterRequest;
import cn.tyt.llmproxy.dto.response.UserLoginResponse;
import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.math.BigDecimal;
import java.util.List;

public interface IUserService extends UserDetailsService {

    /**
     * 用户注册，默认角色为 'user'
     * @param request 注册请求
     * @return 创建的用户实体
     */
    User register(UserRegisterRequest request);

    /**
     * 用户登录
     * @param request 登录请求
     * @return 包含 token 和用户信息的响应
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * [ROOT_ADMIN 专属] 为用户分配角色
     * @param userId 要分配角色的用户ID
     * @param role   目标角色 (e.g., "model_admin", "user")
     */
    void assignRole(Integer userId, String role);

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
    void deleteMyAccessKey(Integer keyId);

    /**
     * 修改当前登录用户的密码
     * @param request 包含旧密码和新密码的请求体
     */
    void changePassword(UserChangePasswordRequest request);

    void creditUserBalance(Integer userId, BigDecimal amount);

    public User getCurrentUser();
}