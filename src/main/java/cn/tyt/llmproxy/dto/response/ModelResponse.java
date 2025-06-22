package cn.tyt.llmproxy.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ModelResponse {
    private Integer id;
    private String displayName;
    private String modelIdentifier;
    private String urlBase;
    // 注意：不返回 apiKey
    private List<String> capabilities;
    private Integer priority;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}