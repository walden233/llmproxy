package cn.tyt.llmproxy.config;

import cn.tyt.llmproxy.filter.JwtAuthenticationTokenFilter;
import cn.tyt.llmproxy.service.IAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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
    private IAdminService adminService; // UserDetailsService 实现

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

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
                // 关闭 CSRF，因为我们使用 JWT，是无状态的
                .csrf(AbstractHttpConfigurer::disable)
                // 不通过 Session 获取 SecurityContext
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 对于登录和注册接口，允许匿名访问
                        .requestMatchers("/v1/auth/login", "/v1/auth/register").permitAll()
//                        // 允许 OPTIONS 请求 (用于CORS预检)
//                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        // 除上面外的所有请求全部需要鉴权认证
                        .anyRequest().authenticated()
                )
                // 添加 JWT filter
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        // 可以配置自定义的认证失败和授权失败处理器
        // .exceptionHandling(exceptions -> exceptions
        //     .authenticationEntryPoint(unauthorizedHandler)
        //     .accessDeniedHandler(accessDeniedHandler)
        // );

        return http.build();
    }
}