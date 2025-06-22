package cn.tyt.llmproxy.controller; // 确保包名正确

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest // 加载完整的 Spring 应用上下文
@AutoConfigureMockMvc // 自动配置 MockMvc，用于模拟 HTTP 请求
@ActiveProfiles("test") // 激活测试环境的配置文件 (application-test.yml)
@Transactional // 默认情况下，每个测试方法执行后事务会回滚
public abstract class BaseTest {

    @Autowired
    protected MockMvc mockMvc; // 用于执行 HTTP 请求

    @Autowired
    protected ObjectMapper objectMapper; // 用于 JSON 序列化/反序列化

    protected String adminToken; // 用于存储登录后获取的 token

    // 可以添加一些通用的辅助方法，比如登录并获取 token
}