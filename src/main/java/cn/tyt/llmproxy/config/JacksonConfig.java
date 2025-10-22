package cn.tyt.llmproxy.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    // 定义全局日期时间格式
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {

        return builder -> {

            // 1. 创建 JavaTimeModule 用于处理 Java 8 日期时间
            JavaTimeModule javaTimeModule = new JavaTimeModule();

            // 2. 添加 LocalDateTime 的序列化器和反序列化器
            javaTimeModule.addSerializer(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
            javaTimeModule.addDeserializer(LocalDateTime.class,
                    new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));

            // 3. 添加 LocalDate 的序列化器和反序列化器
            javaTimeModule.addSerializer(LocalDate.class,
                    new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
            javaTimeModule.addDeserializer(LocalDate.class,
                    new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));

            // 4. 添加 LocalTime 的序列化器和反序列化器
            javaTimeModule.addSerializer(LocalTime.class,
                    new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));
            javaTimeModule.addDeserializer(LocalTime.class,
                    new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

            // 5. 注册模块
            builder.modules(javaTimeModule);

            // 6. 更多自定义设置

//            // 设置在序列化时，只包含非空(NON_NULL)的字段
//            builder.serializationInclusion(JsonInclude.Include.NON_NULL);

            // 设置在反序列化时，如果遇到未知的属性(JSON中有，Java对象中没有)，则忽略，不抛出异常
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // 也可以通过 .featuresToEnable(...) 来启用特性
            // 例如：启用美观的缩进输出
            // builder.featuresToEnable(SerializationFeature.INDENT_OUTPUT);
        };
    }

    /*
     * 警告：不推荐使用下面的方法来完全覆盖 ObjectMapper
     * * @Bean
     * @Primary
     * public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
     * // 像这样创建 ObjectMapper 会使 Spring Boot 的很多默认配置失效
     * // (例如，对 Page, Resource 等类型的自动支持)
     * ObjectMapper objectMapper = builder.createXmlMapper(false).build();
     * * // ... 在这里进行自定义 ...
     * * return objectMapper;
     * }
     * * 始终优先使用 Jackson2ObjectMapperBuilderCustomizer
     */
}