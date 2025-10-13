package cn.tyt.llmproxy.dto.request;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ModelUpdateRequest {
    private String displayName;
    private String modelIdentifier; // 通常不建议修改，如果修改需要保证唯一性
//    private String urlBase;
//    private String apiKey; // 留空则不更新
    private List<String> capabilities;
    private Map<String, Object> pricing;
    private Integer providerId;
    @Min(value = 1, message = "优先级最小为1")
    private Integer priority;
}