package cn.tyt.llmproxy.dto.openai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Allows the {@code content} field to accept both string and array styles used by OpenAI.
 */
public class OpenAiMessageContentDeserializer extends JsonDeserializer<List<OpenAiMessageContent>> {

    @Override
    public List<OpenAiMessageContent> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = p.currentToken();
        if (currentToken == JsonToken.VALUE_NULL) {
            return Collections.emptyList();
        }
        JsonNode node = p.readValueAsTree();
        if (node == null || node.isNull()) {
            return Collections.emptyList();
        }
        if (node.isTextual()) {
            return Collections.singletonList(OpenAiMessageContent.textContent(node.asText()));
        }
        if (node.isArray()) {
            return readArray(node);
        }
        if (node.isObject()) {
            return Collections.singletonList(parseContentNode(node));
        }
        return Collections.singletonList(OpenAiMessageContent.textContent(node.toString()));
    }

    private List<OpenAiMessageContent> readArray(JsonNode arrayNode) {
        List<OpenAiMessageContent> contents = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            contents.add(parseContentNode(elements.next()));
        }
        return contents;
    }

    private OpenAiMessageContent parseContentNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return OpenAiMessageContent.textContent("");
        }
        String type = node.path("type").asText("text");
        if ("image_url".equals(type) || "input_image".equals(type)) {
            JsonNode imageNode = node.path("image_url");
            OpenAiImageUrl imageUrl = OpenAiImageUrl.builder()
                    .url(imageNode.path("url").asText(null))
                    .detail(imageNode.path("detail").asText(null))
                    .base64Json(imageNode.path("b64_json").asText(null))
                    .mimeType(imageNode.path("mime_type").asText(null))
                    .build();
            return OpenAiMessageContent.imageContent(imageUrl);
        }
        if ("input_text".equals(type)) {
            return OpenAiMessageContent.textContent(node.path("text").asText(""));
        }
        return OpenAiMessageContent.textContent(node.path("text").asText(node.toString()));
    }
}
