package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.AdminLoginRequest;
import cn.tyt.llmproxy.dto.request.AdminRegisterRequest;
import cn.tyt.llmproxy.dto.response.AdminLoginResponse;
import cn.tyt.llmproxy.entity.Admin;
import cn.tyt.llmproxy.service.IAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AdminController {

    @Autowired
    private IAdminService adminService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody AdminRegisterRequest request) {
        Admin admin = adminService.register(request);
        // 通常注册成功不直接返回敏感信息，可以返回一个简单的成功消息
        return Result.success("用户注册成功", admin.getUsername());
    }

    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminService.login(request);
        return Result.success(response);
    }
}