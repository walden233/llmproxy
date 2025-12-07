package cn.tyt.llmproxy.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationSummaryDto {
    private String conversationId;
    private String title;
    private Boolean pinned;
    private String lastModelIdentifier;
    private String lastMessageSummary;
    private Integer messageCount;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
