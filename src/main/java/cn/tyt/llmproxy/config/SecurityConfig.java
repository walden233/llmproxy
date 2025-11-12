package cn.tyt.llmproxy.config;

import cn.tyt.llmproxy.filter.JwtAuthenticationTokenFilter;
import cn.tyt.llmproxy.security.*;
import cn.tyt.llmproxy.service.IUserService;
import cn.tyt.llmproxy.security.Roles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 开启方法级别的权限控制
public class SecurityConfig {

    @Autowired
    private IUserService userService; // 注入 IUserService
    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    @Autowired
    private RestAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private RestfulAccessDeniedHandler accessDeniedHandler;

    // 密码编码器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 获取 AuthenticationManager (原 configure(AuthenticationManagerBuilder auth) 的替代)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许来自任何来源的请求（在生产环境中应该更具体，例如 "http://your-frontend-domain.com"）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        // 允许所有标准的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 允许浏览器发送凭证（如 cookies）
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有 URL 应用这个 CORS 配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 公开访问的端点
                        .requestMatchers("/v1/auth/login", "/v1/auth/register", "/v1/chat", "/v1/generate-image","v1/async/chat","v1/async/generate-image").permitAll()
                        // 2. 仅限 MODEL_ADMIN 和 ROOT_ADMIN 访问的模型管理端点
                        .requestMatchers("/v1/models/**").hasAnyAuthority(Roles.MODEL_ADMIN, Roles.ROOT_ADMIN)
//                        // 3. 仅限 ROOT_ADMIN 访问的用户管理/权限分配端点 (假设有)
//                        .requestMatchers("/v1/admin/users/**").hasAuthority(Roles.ROOT_ADMIN)

                        // 4. 允许 OPTIONS 请求 (用于CORS预检)
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()

                        // 5. 其他所有需要认证的请求 (例如 /v1/auth/access-keys, /v1/me/** 等)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)//这个比上面的先执行
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}
