package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.mapper.AccessKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.tyt.llmproxy.common.domain.LoginUser;
import cn.tyt.llmproxy.common.enums.StatusEnum;
import cn.tyt.llmproxy.common.utils.JwtTokenUtil;
import cn.tyt.llmproxy.dto.request.AdminLoginRequest;
import cn.tyt.llmproxy.dto.request.AdminRegisterRequest;
import cn.tyt.llmproxy.dto.response.AdminLoginResponse;
import cn.tyt.llmproxy.entity.Admin;
import cn.tyt.llmproxy.mapper.AdminMapper;
import cn.tyt.llmproxy.service.IAdminService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext; // 引入 ApplicationContext
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList; // 用于 UserDetails 的权限列表
import java.util.List;
import java.util.UUID;

@Service
public class AdminServiceImpl implements IAdminService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private AccessKeyMapper accessKeyMapper;

//    @Autowired
//    private PasswordEncoder passwordEncoder;

//    @Autowired
//    private AuthenticationManager authenticationManager; // 用于登录认证
    //解决循环依赖
    @Autowired
    private ApplicationContext applicationContext;

    private AuthenticationManager getAuthenticationManager() {
        return applicationContext.getBean(AuthenticationManager.class);
    }

    private PasswordEncoder getPasswordEncoder() {
        return applicationContext.getBean(PasswordEncoder.class);
    }


    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    @Transactional
    public Admin register(AdminRegisterRequest request) {
        // 检查用户名是否已存在
        if (adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, request.getUsername())) != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 检查邮箱是否已存在 (如果邮箱是唯一约束)
        if (request.getEmail() != null && adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getEmail, request.getEmail())) != null) {
            throw new RuntimeException("邮箱已存在");
        }

        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setPasswordHash(getPasswordEncoder().encode(request.getPassword()));
        admin.setEmail(request.getEmail());
        admin.setStatus(StatusEnum.AVAILABLE.getCode()); // 默认可用
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        adminMapper.insert(admin);
        return admin;
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        try {
            // 使用 AuthenticationManager 进行用户认证
            Authentication authentication = getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            // 认证成功后，将 Authentication 对象存入 SecurityContextHolder
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 生成 JWT
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(loginUser.getUsername());

            return new AdminLoginResponse(token, loginUser.getUsername());

        } catch (BadCredentialsException e) {
            throw new RuntimeException("用户名或密码错误");
        }
    }

    // 实现 UserDetailsService 的方法，用于 Spring Security 从数据库加载用户信息
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
        if (admin == null) {
            throw new UsernameNotFoundException("用户: " + username + " 不存在");
        }
        if (admin.getStatus() == StatusEnum.UNAVAILABLE.getCode()) {
            throw new RuntimeException("用户: " + username + " 已被禁用");
        }
        // 返回 Spring Security 的 UserDetails 实现，这里用我们自定义的 LoginUser
        // 权限列表可以根据实际业务添加，这里简单给一个空列表或默认角色
        return new LoginUser(admin, new ArrayList<>());
    }

    @Override
    public AccessKey createAccessKey() {
        // 从安全上下文中获取当前管理员ID
        Admin currentUser = getCurrentAdmin();

        // 1. 生成一个唯一的、难以猜测的Key
        String key = "ak-" + UUID.randomUUID().toString().replace("-", "");

        // 2. 构建AccessKey实体
        AccessKey newAccessKey = AccessKey.builder()
                .keyValue(key)
                .adminId(currentUser.getId()) // 使用获取到的ID
                .isActive(1)
                .build();

        // 3. 插入数据库
        accessKeyMapper.insert(newAccessKey);

        // 4. 返回创建的实体
        return newAccessKey;
    }

    /**
     * 获取当前登录用户的所有AccessKey
     */
    @Override
    public List<AccessKey> getAccessKeys() {
        // 从安全上下文中获取当前管理员ID
        Admin currentUser = getCurrentAdmin();

        QueryWrapper<AccessKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("admin_id", currentUser.getId());
        return accessKeyMapper.selectList(queryWrapper);
    }


    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in security context.");
        }

        Object principal = authentication.getPrincipal();

        // 根据你的JWT过滤器代码，principal就是UserDetails对象。
        // 而你的IAdminService继承了UserDetailsService，所以loadUserByUsername返回的是Admin对象。
        // 因此，这里的principal可以直接转换为Admin。
        if (principal instanceof LoginUser) {
            return ((LoginUser) principal).getAdmin();
        } else {
            // 如果principal是其他类型（例如String），你可能需要根据username重新查询数据库。
            // 但根据你的设置，直接转换是最高效的。
            throw new IllegalStateException("The principal is not an instance of LoginUser. Check your security configuration.");
        }
    }
}