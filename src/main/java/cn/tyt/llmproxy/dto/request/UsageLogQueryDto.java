package cn.tyt.llmproxy.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 自定义用量日志查询参数，支持按多条件筛选。
 */
@Data
public class UsageLogQueryDto {
    private Integer userId;
    private Integer accessKeyId;
    private Integer modelId;
    private Boolean isSuccess;
    private Boolean isAsync;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 返回条数限制，null 或 <=0 表示不限制。
     */
    private Integer limit;

    /**
     * 是否按照 createTime 倒序，默认升序。
     */
    private Boolean sortDesc;
}
