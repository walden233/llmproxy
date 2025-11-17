package cn.tyt.llmproxy.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single content block inside an OpenAI style message.
 * Only the commonly used text and image_url types are modeled for now.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiMessageContent {

    private String type;
    private String text;

    @JsonProperty("image_url")
    private OpenAiImageUrl imageUrl;

    public static OpenAiMessageContent textContent(String text) {
        return OpenAiMessageContent.builder()
                .type("text")
                .text(text)
                .build();
    }

    public static OpenAiMessageContent imageContent(OpenAiImageUrl imageUrl) {
        return OpenAiMessageContent.builder()
                .type("image_url")
                .imageUrl(imageUrl)
                .build();
    }
}
