package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.request.AdminLoginRequest;
import cn.tyt.llmproxy.dto.request.AdminRegisterRequest;
import cn.tyt.llmproxy.dto.response.AdminLoginResponse;
import cn.tyt.llmproxy.entity.Admin;
import org.springframework.security.core.userdetails.UserDetailsService;


public interface IAdminService extends UserDetailsService { // 继承 UserDetailsService 以便 Spring Security 使用
    Admin register(AdminRegisterRequest request);
    AdminLoginResponse login(AdminLoginRequest request);
}