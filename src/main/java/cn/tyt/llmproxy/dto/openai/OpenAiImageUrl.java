package cn.tyt.llmproxy.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiImageUrl {

    private String url;

    /**
     * Matches OpenAI's optional detail field (auto, low, high).
     */
    private String detail;

    /**
     * Some SDKs send base64 images via b64_json (OpenAI image API style).
     */
    @JsonProperty("b64_json")
    private String base64Json;

    @JsonProperty("mime_type")
    private String mimeType;
}
