package cn.tyt.llmproxy.security;

import cn.tyt.llmproxy.common.enums.ResultCode;
import cn.tyt.llmproxy.common.domain.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义权限不足处理逻辑，当用户访问没有权限的资源时，返回403
 */
@Component
public class RestfulAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 状态码 403

        // 使用 ResultCode 枚举来构建标准化的返回信息
        Result<Object> result = Result.error(ResultCode.PERMISSION_DENIED.getCode(),"无权限");

        // 使用Jackson将对象转换为JSON字符串并写入响应
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().println(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }
}