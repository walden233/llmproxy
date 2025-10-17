package cn.tyt.llmproxy.common.domain;

import cn.tyt.llmproxy.entity.User; // 引入 User
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;

public class LoginUser implements UserDetails, Serializable {

    // 提供获取 User 对象的方法
    @Getter
    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public LoginUser(User user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // 以下方法可以根据 user.status 来实现
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return User.STATUS_ACTIVE.equals(user.getStatus());
    }



    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return User.STATUS_ACTIVE.equals(user.getStatus());
    }
}