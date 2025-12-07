package cn.tyt.llmproxy.dto.request;

import lombok.Data;

@Data
public class UpdateConversationRequest {
    private String title;
    private Boolean pinned;
}
