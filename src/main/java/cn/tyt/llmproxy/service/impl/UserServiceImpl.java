package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.common.domain.LoginUser;
import cn.tyt.llmproxy.common.enums.RoleEnum;
import cn.tyt.llmproxy.common.utils.JwtTokenUtil;
import cn.tyt.llmproxy.dto.request.UserChangePasswordRequest;
import cn.tyt.llmproxy.dto.request.UserLoginRequest;
import cn.tyt.llmproxy.dto.request.UserRegisterRequest;
import cn.tyt.llmproxy.dto.response.UserLoginResponse;
import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.entity.User;
import cn.tyt.llmproxy.mapper.AccessKeyMapper;
import cn.tyt.llmproxy.mapper.UserMapper;
import cn.tyt.llmproxy.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AccessKeyMapper accessKeyMapper;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private AuthenticationManager getAuthenticationManager() {
        return applicationContext.getBean(AuthenticationManager.class);
    }

    private PasswordEncoder getPasswordEncoder() {
        return applicationContext.getBean(PasswordEncoder.class);
    }

    @Override
    @Transactional
    public User register(UserRegisterRequest request) {
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) != null) {
            throw new RuntimeException("用户名已存在");
        }
        if (request.getEmail() != null && userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())) != null) {
            throw new RuntimeException("邮箱已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(getPasswordEncoder().encode(request.getPassword()));
        user.setEmail(request.getEmail());
        // 注册用户默认为 'user' 角色
        user.setRole(RoleEnum.USER.getValue());
        user.setBalance(new BigDecimal("0.00")); // 初始余额为0
        user.setStatus(User.STATUS_ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        try {
            Authentication authentication = getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(loginUser.getUsername());

            return new UserLoginResponse(token, loginUser.getUsername(), loginUser.getUser().getRole());
        } catch (BadCredentialsException e) {
            throw new RuntimeException("用户名或密码错误");
        }
    }

    @Override
    //todo:添加redis缓存
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户: " + username + " 不存在");
        }
        if (User.STATUS_INACTIVE.equals(user.getStatus())) {
            throw new RuntimeException("用户: " + username + " 已被禁用");
        }
        // **核心改造点：将角色信息封装为 GrantedAuthority**
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
        return new LoginUser(user, authorities);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('root_admin')") // **权限控制**
    public void assignRole(Integer userId, String role) {
        // 校验角色是否合法
        if (!RoleEnum.contains(role)) {
            throw new IllegalArgumentException("无效的角色: " + role);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // root_admin 不能被降级
        if (RoleEnum.ROOT_ADMIN.getValue().equals(user.getRole())) {
            throw new SecurityException("不能修改 root_admin 的角色");
        }

        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public AccessKey createAccessKey() {
        User currentUser = getCurrentUser();
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
        User currentUser = getCurrentUser();
        return accessKeyMapper.selectList(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getUserId, currentUser.getId()));
    }

    @Override
    @Transactional
    public void deleteMyAccessKey(Integer keyId) {
        User currentUser = getCurrentUser();
        AccessKey accessKeyToDelete = accessKeyMapper.selectById(keyId);
        if (accessKeyToDelete == null || !accessKeyToDelete.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Access Key not found or you don't have permission to delete it.");
        }
        accessKeyMapper.deleteById(keyId);
    }

    @Override
    @Transactional
    public void changePassword(UserChangePasswordRequest request) {
        User currentUser = getCurrentUser();
        if (!getPasswordEncoder().matches(request.getOldPassword(), currentUser.getPasswordHash())) {
            throw new BadCredentialsException("旧密码不正确");
        }
        currentUser.setPasswordHash(getPasswordEncoder().encode(request.getNewPassword()));
        currentUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(currentUser);
    }
    @Override
    public void creditUserBalance(Integer userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            // 使用自定义的业务异常更佳
            throw new RuntimeException("User not found with id: " + userId);
        }
        user.setBalance(user.getBalance().add(amount));
        userMapper.updateById(user);
    }
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            return ((LoginUser) principal).getUser();
        }
        throw new IllegalStateException("The principal is not an instance of LoginUser.");
    }
}