package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "llm_models", autoResultMap = true)
public class LlmModel {
    // 状态常量
    public static final Integer STATUS_ONLINE = 1;
    public static final Integer STATUS_OFFLINE = 0;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer providerId;
    private String displayName;
    private String modelIdentifier;
    private Integer priority;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> capabilities;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> pricing;

    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}