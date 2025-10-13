package cn.tyt.llmproxy.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class LlmModelConfigDto {

    // --- LlmModel 的核心信息 ---
    private Integer id;
    private String displayName;
    private String modelIdentifier;
    private Integer priority;
    private List<String> capabilities;
    private Map<String, Object> pricing;

    // --- Provider 的信息 ---
    private String providerName;
    private String urlBase;

    // --- ProviderKey 的信息 ---
    private String apiKey; // 我们假设一个模型配置只对应一个有效的 key
}