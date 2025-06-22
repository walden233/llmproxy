package cn.tyt.llmproxy.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ModelCreateRequest {
    @NotBlank(message = "模型显示名称不能为空")
    private String displayName;

    @NotBlank(message = "模型标识不能为空")
    private String modelIdentifier;

    @NotBlank(message = "模型URL Base不能为空")
    private String urlBase;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    @NotEmpty(message = "模型能力列表不能为空")
    private List<String> capabilities; // e.g., ["text-to-text"]

    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级最小为1")
    private Integer priority;
}