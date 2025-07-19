package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.AdminLoginRequest;
import cn.tyt.llmproxy.dto.request.AdminRegisterRequest;
import cn.tyt.llmproxy.dto.response.AdminLoginResponse;
import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.entity.Admin;
import cn.tyt.llmproxy.service.IAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 为当前登录的用户创建一个新的Access Key
     */
    @PostMapping("/access-keys")
    public Result<AccessKey> createAccessKey() {
        AccessKey accessKey = adminService.createAccessKey();
        return Result.success(accessKey);
    }

    /**
     * 获取当前用户的所有Access Key
     */
    @GetMapping("/access-keys")
    public Result<List<AccessKey>> listAccessKeys() {
        List<AccessKey> keys = adminService.getAccessKeys();
        return Result.success(keys);
    }

    /**
     * 删除当前用户的一个指定Access Key
     * @param id 要删除的Access Key的ID
     */
    @DeleteMapping("/access-keys/{id}")
    public Result<Void> deleteAccessKey(@PathVariable Integer id) {
        adminService.deleteMyAccessKey(id);
        return Result.success(); // 删除成功，返回一个没有数据体的成功响应
    }
}