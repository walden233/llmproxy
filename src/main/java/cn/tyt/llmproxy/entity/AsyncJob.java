package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "async_jobs", autoResultMap = true)
public class AsyncJob {
    // 状态常量
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String jobId;
    private Integer userId;
    private Integer accessKeyId;
    private String modelName;
    private String status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> requestPayload;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> resultPayload;

    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}