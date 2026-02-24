package cn.tyt.llmproxy.dto.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiChatRequest {

    /**
     * Optional idempotency key for billing.
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * Aligns to OpenAI's model field; mapped to LlmModel.modelIdentifier.
     */
    @NotBlank
    private String model;

    @NotEmpty
    private List<OpenAiMessage> messages;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    private List<String> stop;

    private Boolean stream;

    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    private List<OpenAiTool> tools;

    @JsonProperty("tool_choice")
    private JsonNode toolChoice;

    @JsonProperty("response_format")
    private OpenAiResponseFormat responseFormat;

    private Map<String, Object> metadata;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("persist_history")
    private Boolean persistHistory;

    /**
     * Allows vendor specific overrides without polluting the schema.
     */
    @JsonProperty("extra_params")
    private Map<String, Object> extraParams;
}
