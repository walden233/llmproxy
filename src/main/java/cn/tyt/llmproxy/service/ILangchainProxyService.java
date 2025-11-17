package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.openai.OpenAiChatRequest;
import cn.tyt.llmproxy.dto.openai.OpenAiChatResponse;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ChatResponse_dto;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ILangchainProxyService {
    ChatResponse_dto chat(ChatRequest_dto request, Integer userId, Integer accessKeyId, Boolean isAsync);
    OpenAiChatResponse chatV2(OpenAiChatRequest request, Integer userId, Integer accessKeyId, Boolean isAsync);
    SseEmitter chatV2Stream(OpenAiChatRequest request, Integer userId, Integer accessKeyId, Boolean isAsync);
    ImageGenerationResponse generateImage(ImageGenerationRequest request, Integer userId, Integer accessKeyId, Boolean isAsync);
    // 可以添加流式聊天接口
    // StreamingChatResponse streamChat(ChatRequest request, StreamingResponseHandler<AiMessage> handler);
}
