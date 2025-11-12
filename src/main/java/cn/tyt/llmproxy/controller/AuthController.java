package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.UserAssignRoleRequest;
import cn.tyt.llmproxy.dto.request.UserChangePasswordRequest;
import cn.tyt.llmproxy.dto.request.UserLoginRequest;
import cn.tyt.llmproxy.dto.request.UserRegisterRequest;
import cn.tyt.llmproxy.dto.response.UserLoginResponse;
import cn.tyt.llmproxy.entity.User;
import cn.tyt.llmproxy.service.IAccessKeyService;
import cn.tyt.llmproxy.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private IUserService userService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserRegisterRequest request) {
        User user = userService.register(request);
        // 通常注册成功不直接返回敏感信息，可以返回一个简单的成功消息
        return Result.success("用户注册成功", user.getUsername());
    }

    @PostMapping("/login")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /**
     * 修改当前登录用户的密码
     * @param request 包含旧密码和新密码
     * @return 成功消息
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(@Valid @RequestBody UserChangePasswordRequest request) {
        userService.changePassword(request);
        // 提示用户密码已修改，建议重新登录，因为旧的token仍然在有效期内
        return Result.success("密码修改成功，为了安全建议您重新登录。");
    }

    /**
     * 为用户分配角色 (仅限 ROLE_ROOT_ADMIN)
     * <p>
     * Controller层负责拦截HTTP请求，Service层确保业务逻辑在任何调用场景下（如内部调用）都是安全的。
     *
     * @param request 包含用户ID和新角色的请求体
     * @return 操作成功的响应
     */
    @PostMapping("/assign-role")
    @PreAuthorize("hasAuthority('ROLE_ROOT_ADMIN')")
    public Result<?> assignRole(@Valid @RequestBody UserAssignRoleRequest request) {
        userService.assignRole(request.getUserId(), request.getRole());
        return Result.success("角色分配成功");
    }
}
