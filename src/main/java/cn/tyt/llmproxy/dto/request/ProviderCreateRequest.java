package cn.tyt.llmproxy.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ProviderCreateRequest {
    @NotBlank(message = "供应商名称不能为空")
    private String name;
    private String urlBase;
}