package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ChatResponse_dto;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;

public interface ILangchainProxyService {
    ChatResponse_dto chat(ChatRequest_dto request);
    ImageGenerationResponse generateImage(ImageGenerationRequest request);
    // 可以添加流式聊天接口
    // StreamingChatResponse streamChat(ChatRequest request, StreamingResponseHandler<AiMessage> handler);
}