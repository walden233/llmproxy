package cn.tyt.llmproxy.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ModelStatisticsDto {
    private LocalDate date;
    private Integer modelId;
    private String modelIdentifier;
    private Integer totalRequests;
    private Integer successCount;
    private Integer failureCount;
}