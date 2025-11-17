package cn.tyt.llmproxy.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiJsonSchema {

    private String name;

    /**
     * The actual JSON schema definition as a Map.
     */
    private Map<String, Object> schema;

    /**
     * OpenAI optional strict flag.
     */
    private Boolean strict;

    @JsonProperty("description")
    private String description;
}
