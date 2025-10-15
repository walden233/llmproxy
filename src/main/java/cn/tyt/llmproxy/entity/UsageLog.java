package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("usage_logs")
//todo:使用mongoDB记录
public class UsageLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Integer accessKeyId;
    private Integer modelId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer imageCount;
    private BigDecimal cost;
    private LocalDateTime requestTimestamp;
    private LocalDateTime responseTimestamp;
    private Boolean isSuccess;
}