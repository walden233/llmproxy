package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.common.domain.LoginUser;
import cn.tyt.llmproxy.common.enums.ResultCode;
import cn.tyt.llmproxy.common.exception.BusinessException;
import cn.tyt.llmproxy.common.utils.JwtTokenUtil;
import cn.tyt.llmproxy.dto.request.UserChangeNameRequest;
import cn.tyt.llmproxy.dto.request.UserChangePasswordRequest;
import cn.tyt.llmproxy.dto.request.UserLoginRequest;
import cn.tyt.llmproxy.dto.request.UserRegisterRequest;
import cn.tyt.llmproxy.dto.response.UserLoginResponse;
import cn.tyt.llmproxy.dto.response.UserProfileResponse;
import cn.tyt.llmproxy.entity.User;
import cn.tyt.llmproxy.mapper.UserMapper;
import cn.tyt.llmproxy.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
import cn.tyt.llmproxy.security.Roles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;
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
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "用户名已存在");
        }
        if (request.getEmail() != null && userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())) != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "邮箱已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(getPasswordEncoder().encode(request.getPassword()));
        user.setEmail(request.getEmail());
        // 注册用户默认为 'ROLE_USER' 角色
        user.setRole(Roles.USER);
        user.setBalance(new BigDecimal("1.00")); // 初始余额为1
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
            throw new BusinessException(ResultCode.AUTH_ERROR, "用户名或密码错误");
        }
    }

    @Override
    @Cacheable(value = "users", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户: " + username + " 不存在");
        }
        if (User.STATUS_INACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "用户: " + username + " 已被禁用");
        }
        // **核心改造点：将角色信息封装为 GrantedAuthority**
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
        return new LoginUser(user, authorities);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ROOT_ADMIN')") // **权限控制**
    @CachePut(value = "users", key = "#result.username")//缓存一致性
    public User assignRole(Integer userId, String role) {
        // 校验角色是否合法
        if (!Roles.contains(role)) {
            throw new IllegalArgumentException("无效的角色: " + role);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }

        // ROLE_ROOT_ADMIN 不能被降级
        if (Roles.ROOT_ADMIN.equals(user.getRole())) {
            throw new SecurityException("不能修改 ROLE_ROOT_ADMIN 的角色");
        }

        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
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


    //有问题：getCurrentUser拿到的缓存中的user的balance是不一致的。
    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.username")//缓存一致
    public User changeName(UserChangeNameRequest request) {
        User currentUser = getCurrentUser();
        currentUser.setUsername(request.getNewName());
        currentUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(currentUser);
        return currentUser;
    }

    @Override
    public void creditUserBalance(Integer userId, BigDecimal amount) {
        int updated = userMapper.updateBalance(userId, amount);
        if (updated == 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "Failed to update balance due to concurrency conflict.");
        }
    }
    @Override
    public User getCurrentUser() {
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

    @Override
    public IPage<UserProfileResponse> findAllUsers(int pageNum, int pageSize, String role, String sortBy, String sortOrder) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }

        if (sortBy != null && !sortBy.isEmpty()) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            switch (sortBy) {
                case "username":
                    wrapper.orderBy(true, isAsc, User::getUsername);
                    break;
                case "email":
                    wrapper.orderBy(true, isAsc, User::getEmail);
                    break;
                case "role":
                    wrapper.orderBy(true, isAsc, User::getRole);
                    break;
                case "balance":
                    wrapper.orderBy(true, isAsc, User::getBalance);
                    break;
                default:
                    wrapper.orderBy(true, isAsc, User::getId);
                    break;
            }
        } else {
            wrapper.orderByDesc(User::getId);
        }

        IPage<User> userPage = userMapper.selectPage(page, wrapper);

        return userPage.convert(UserProfileResponse::fromEntity);
    }
}
