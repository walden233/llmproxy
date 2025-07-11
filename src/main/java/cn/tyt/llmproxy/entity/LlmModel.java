package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler; // 用于 JSON 类型处理
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "llm_models", autoResultMap = true) // autoResultMap = true 对 JacksonTypeHandler 很重要
public class LlmModel {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String displayName;
    private String modelIdentifier;
    private String urlBase;
    private String apiKey; // 注意：此字段敏感，通常不在查询中直接暴露给前端

    @TableField(typeHandler = JacksonTypeHandler.class) // 告诉 MyBatis-Plus 如何处理 List<String> 与 JSON 互转
    private List<String> capabilities; // 例如: ["text-to-text", "image-to-text"]

    private Integer priority;
    private Integer status; // 0: 下线, 1: 上线
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}