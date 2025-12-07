package cn.tyt.llmproxy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse_dto {
    private String assistantMessage;
    private String usedModelIdentifier; // 实际使用的模型标识
    private Integer inputTokensCount;
    private Integer outputTokensCount;
    private String conversationId;
    private String messageId;
}
