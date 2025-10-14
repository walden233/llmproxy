package cn.tyt.llmproxy.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ModelResponse {
    private Integer id;
    private String displayName;
    private String modelIdentifier;
    // 注意：不返回 apiKey
    private List<String> capabilities;
    private Integer priority;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 从关联的 Provider 获取的字段
    private String providerId;
    private String providerName;
    private String urlBase;
}