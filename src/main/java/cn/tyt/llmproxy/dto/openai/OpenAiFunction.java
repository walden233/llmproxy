package cn.tyt.llmproxy.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class OpenAiFunction {
    private String name;
    private String description;
    /**
     * JSON schema describing the arguments. The schema is stored as a generic map
     * and converted to LangChain4j JsonSchemaElement right before dispatching requests.
     */
    private Map<String, Object> parameters;
}
