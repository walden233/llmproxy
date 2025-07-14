package cn.tyt.llmproxy.security;

import cn.tyt.llmproxy.common.enums.ResultCode;
import cn.tyt.llmproxy.common.domain.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义未授权处理逻辑，当用户未登录或token失效时访问接口，返回401
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 状态码 401

        // 使用 ResultCode 枚举来构建标准化的返回信息
        Result<Object> result = Result.error(ResultCode.UNAUTHORIZED.getCode(), authException.getMessage());

        // 如果想用固定的提示信息，可以这样：
        // Result<Object> result = Result.fail(ResultCode.UNAUTHORIZED);

        // 使用Jackson将对象转换为JSON字符串并写入响应
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().println(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }
}