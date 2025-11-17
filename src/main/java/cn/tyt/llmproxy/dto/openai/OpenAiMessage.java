package cn.tyt.llmproxy.dto.openai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiMessage {

    @NotBlank
    private String role;

    private String name;

    @JsonProperty("tool_call_id")
    @JsonAlias("function_call_id")
    private String toolCallId;

    @JsonProperty("content")
    @JsonDeserialize(using = OpenAiMessageContentDeserializer.class)
    private List<OpenAiMessageContent> contents = Collections.emptyList();

    @JsonProperty("tool_calls")
    private List<OpenAiToolCall> toolCalls;
}
