package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("model_daily_stats")
public class ModelDailyStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer modelId;
    private String modelIdentifier;
    private LocalDate statDate;
    private Integer totalRequests;
    private Integer successCount;
    private Integer failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}