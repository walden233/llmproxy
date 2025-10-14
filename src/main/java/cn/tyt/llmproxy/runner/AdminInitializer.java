package cn.tyt.llmproxy.runner;

import cn.tyt.llmproxy.common.enums.RoleEnum;
import cn.tyt.llmproxy.entity.User;
import cn.tyt.llmproxy.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 检查 root_admin 是否已存在
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, RoleEnum.ROOT_ADMIN.getValue())) == 0) {
            System.out.println("Creating initial root_admin user...");
            User rootAdmin = new User();
            rootAdmin.setUsername("root");
            rootAdmin.setPasswordHash(passwordEncoder.encode("YourSecurePassword123")); // **请务必修改为强密码**
            rootAdmin.setEmail("root@example.com");
            rootAdmin.setRole(RoleEnum.ROOT_ADMIN.getValue());
            rootAdmin.setBalance(new BigDecimal("999999.00"));
            rootAdmin.setStatus(User.STATUS_ACTIVE);
            rootAdmin.setCreatedAt(LocalDateTime.now());
            rootAdmin.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(rootAdmin);
            System.out.println("root_admin user created successfully.");
        }
    }
}