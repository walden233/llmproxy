package cn.tyt.llmproxy.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiFunctionCall {
    private String name;
    /**
     * OpenAI may send either a JSON string or an already parsed object/value.
     * We keep it as Object and convert to string only when needed.
     */
    private Object arguments;
}
