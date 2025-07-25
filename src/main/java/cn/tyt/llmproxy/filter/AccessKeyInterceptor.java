package cn.tyt.llmproxy.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.tyt.llmproxy.service.AccessKeyValidationService;
import cn.tyt.llmproxy.common.domain.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class AccessKeyInterceptor implements HandlerInterceptor {

    public static final String ACCESS_KEY_HEADER = "ACCESS-KEY";

    @Autowired
    private AccessKeyValidationService accessKeyValidationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String accessKey = request.getHeader(ACCESS_KEY_HEADER);

        // 在调用服务层之前，就处理掉无效的输入
        if (accessKey == null || accessKey.trim().isEmpty()) {
            sendUnauthorizedResponse(response, "Unauthorized: Missing Access Key.");
            return false; // 直接拦截，不调用service
        }

        // 只有当accessKey确定存在时，才调用被缓存的方法
        if (accessKeyValidationService.isValid(accessKey)) {
            return true; // Key有效，放行
        }

        // Key存在但无效
        sendUnauthorizedResponse(response, "Unauthorized: Invalid Access Key.");
        return false; // 拦截请求
    }

    // 提取一个私有方法来发送错误响应，避免代码重复
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Result<Void> errorResult = Result.error(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResult));
    }
}