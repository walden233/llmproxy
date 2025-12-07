package cn.tyt.llmproxy.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationMessageDto {
    private String messageId;
    private String conversationId;
    private String role;
    private String content;
    private List<String> imageUrls;
    private String modelIdentifier;
    private Integer promptTokens;
    private Integer completionTokens;
    private BigDecimal cost;
    private LocalDateTime createdAt;
}
