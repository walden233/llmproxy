package cn.tyt.llmproxy.common.domain;

import cn.tyt.llmproxy.entity.Admin;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;


@Getter
public class LoginUser implements UserDetails {

    private final Admin admin;
    private final Collection<? extends GrantedAuthority> authorities; // 权限信息

    public LoginUser(Admin admin, Collection<? extends GrantedAuthority> authorities) {
        this.admin = admin;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return admin.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return admin.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 可以根据业务逻辑判断
    }

    @Override
    public boolean isAccountNonLocked() {
        return admin.getStatus() == 1; // 假设 status=1 表示账户未锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 可以根据业务逻辑判断
    }

    @Override
    public boolean isEnabled() {
        return admin.getStatus() == 1; // 假设 status=1 表示账户启用
    }
}