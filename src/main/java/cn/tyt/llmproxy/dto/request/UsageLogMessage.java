package cn.tyt.llmproxy.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsageLogMessage {
    private Integer userId;
    private Integer accessKeyId;
    private Integer modelId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer imageCount;
    private BigDecimal cost;
    private LocalDateTime time;
    private boolean isSuccess;
    private Boolean isAsync;
}
