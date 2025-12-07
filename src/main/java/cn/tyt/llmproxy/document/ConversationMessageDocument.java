package cn.tyt.llmproxy.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "conversation_messages")
public class ConversationMessageDocument {
    @Id
    private String id;

    @Indexed
    private String conversationId;

    @Indexed
    private Integer userId;

    private String role;
    private String content;
    private List<String> imageUrls;
    private String modelIdentifier;
    private Integer promptTokens;
    private Integer completionTokens;
    private BigDecimal cost;

    @Indexed
    private LocalDateTime createdAt;
}
