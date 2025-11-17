package cn.tyt.llmproxy.config;

import cn.tyt.llmproxy.filter.AccessKeyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AccessKeyInterceptor accessKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册AccessKey拦截器
        registry.addInterceptor(accessKeyInterceptor)
                // 指定该拦截器只对这两个路径生效
                .addPathPatterns("/v1/chat", "/v1/generate-image","/v1/async/chat","/v1/async/generate-image","/v1/v2/chat","/v1/v2/chat/stream");
    }
}
