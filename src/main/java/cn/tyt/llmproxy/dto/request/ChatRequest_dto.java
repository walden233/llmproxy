package cn.tyt.llmproxy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest_dto {

    @NotBlank(message = "用户消息不能为空")
    private String userMessage;

    // 可选：指定使用哪个模型 (数据库中的模型ID或标识符)
    // 如果不传，则由后端根据优先级和能力选择
    private String modelInternalId; // 数据库中的 LlmModel.id
    private String modelIdentifier; // 数据库中的 LlmModel.modelIdentifier

    // 可选：传递历史消息，用于多轮对话
    // 简单起见，这里用字符串列表
    private List<String> history; // 例如: ["user: hello", "assistant: hi", "user: how are you?"]

    // 可选：传递给模型的额外参数 (如 temperature, maxTokens)
    // 具体参数取决于模型提供商和 Langchain4j 的实现
    private Map<String, Object> options;

    // 可选：会话ID，用于有状态的对话 (ChatMemory)
    private String sessionId;
}