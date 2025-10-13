package cn.tyt.llmproxy.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class KeyCreateRequest {
    private Integer providerId;
    private String providerName;

    @NotBlank(message = "API Key 不能为空")
    private String apiKey;
}