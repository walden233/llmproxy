package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.common.enums.ModelCapabilityEnum;
import cn.tyt.llmproxy.common.enums.StatusEnum;
import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ChatResponse_dto;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import cn.tyt.llmproxy.entity.LlmModel;
import cn.tyt.llmproxy.mapper.LlmModelMapper;
import cn.tyt.llmproxy.service.ILangchainProxyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
//import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LangchainProxyServiceImpl implements ILangchainProxyService {

    private static final Logger log = LoggerFactory.getLogger(LangchainProxyServiceImpl.class);

    @Autowired
    private LlmModelMapper llmModelMapper;

    // 简单内存会话存储，生产环境可能需要 Redis 或其他持久化存储
    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();

    @Override
    public ChatResponse_dto chat(ChatRequest_dto request) {
        LlmModel selectedModel = selectModel(request.getModelInternalId(), request.getModelIdentifier(), ModelCapabilityEnum.TEXT_TO_TEXT.getValue());
        log.info("使用模型进行聊天: {} (ID: {})", selectedModel.getDisplayName(), selectedModel.getModelIdentifier());

        OpenAiChatModel chatModel = buildChatLanguageModel(selectedModel);

        List<ChatMessage> messages = new ArrayList<>();
        // 添加系统消息 (如果需要)
        // messages.add(SystemMessage.from("You are a helpful assistant."));

        // 处理历史消息
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            for (String historyMessage : request.getHistory()) {
                // 简单解析 "role: content" 格式，可能需要更健壮的解析
                String[] parts = historyMessage.split(":", 2);
                if (parts.length == 2) {
                    if ("user".equalsIgnoreCase(parts[0].trim())) {
                        messages.add(UserMessage.from(parts[1].trim()));
                    } else if ("assistant".equalsIgnoreCase(parts[0].trim()) || "ai".equalsIgnoreCase(parts[0].trim())) {
                        messages.add(AiMessage.from(parts[1].trim()));
                    }
                }
            }
        }
        messages.add(UserMessage.from(request.getUserMessage()));


        ChatResponse response;
        String sessionId = StringUtils.hasText(request.getSessionId()) ? request.getSessionId() : null;

        if (sessionId != null) {
            ChatMemory chatMemory = chatMemories.computeIfAbsent(sessionId, id ->
                    MessageWindowChatMemory.withMaxMessages(10)); // 保留最近10条消息
            // 先加载历史消息到当前 messages 列表，再一起发送给模型
            messages.addAll(0, chatMemory.messages()); // 将记忆消息加到最前面
            chatMemory.add(UserMessage.from(request.getUserMessage())); // 将当前用户消息加入记忆
            response = chatModel.chat(chatMemory.messages()); // 使用记忆中的消息进行生成
            chatMemory.add(response.aiMessage()); // 将AI回复加入记忆
        } else {
            response = chatModel.chat(messages);
        }


        if (response == null || response.aiMessage() == null) {
            throw new RuntimeException("模型未能生成响应。");
        }

        return new ChatResponse_dto(response.aiMessage().text(), selectedModel.getModelIdentifier(), sessionId);
    }


    //暂未使用
    @Override
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        LlmModel selectedModel = selectModel(request.getModelInternalId(), request.getModelIdentifier(), ModelCapabilityEnum.TEXT_TO_IMAGE.getValue());
        log.info("使用模型生成图像: {} (ID: {})", selectedModel.getDisplayName(), selectedModel.getModelIdentifier());

        ImageModel imageModel = buildImageModel(selectedModel, request);

        Response<Image> response = imageModel.generate(request.getPrompt());

        if (response == null || response.content() == null || response.content().url() == null) {
            throw new RuntimeException("模型未能生成图像或图像URL。");
        }

        return new ImageGenerationResponse(
                response.content().url().toString(),
                response.content().revisedPrompt(), // DALL-E 可能会返回修改后的 prompt
                selectedModel.getModelIdentifier()
        );
    }

    private LlmModel selectModel(String internalIdStr, String identifier, String requiredCapability) {
        Optional<LlmModel> modelOpt = Optional.empty();
        if (StringUtils.hasText(internalIdStr)) {
            try {
                Integer internalId = Integer.parseInt(internalIdStr);
                modelOpt = Optional.ofNullable(llmModelMapper.selectById(internalId));
            } catch (NumberFormatException e) {
                log.warn("无效的内部模型ID格式: {}", internalIdStr);
            }
        } else if (StringUtils.hasText(identifier)) {
            modelOpt = Optional.ofNullable(llmModelMapper.selectOne(
                    new LambdaQueryWrapper<LlmModel>().eq(LlmModel::getModelIdentifier, identifier)
            ));
        }

        if (modelOpt.isPresent()) {
            LlmModel model = modelOpt.get();
            if (model.getStatus() != StatusEnum.AVAILABLE.getCode()) {
                throw new RuntimeException("模型 " + model.getDisplayName() + " 当前不可用。");
            }
            if (!model.getCapabilities().contains(requiredCapability)) {
                throw new RuntimeException("模型 " + model.getDisplayName() + " 不支持 " + requiredCapability + " 功能。");
            }
            return model;
        } else {
            // 如果没有指定模型，则按优先级和能力选择
            List<LlmModel> availableModels = llmModelMapper.selectList(
                    new LambdaQueryWrapper<LlmModel>()
                            .eq(LlmModel::getStatus, StatusEnum.AVAILABLE.getCode())
                            .apply("JSON_CONTAINS(capabilities, JSON_QUOTE({0}))", requiredCapability) // MySQL JSON_CONTAINS
                            .orderByAsc(LlmModel::getPriority)
            );
            if (availableModels.isEmpty()) {
                throw new RuntimeException("没有找到支持 " + requiredCapability + " 功能的可用模型。");
            }
            return availableModels.get(0); // 选择优先级最高的
        }
    }

    private OpenAiChatModel buildChatLanguageModel(LlmModel modelConfig) {

//        if (modelConfig.getModelIdentifier().startsWith("gpt-") ||
//                modelConfig.getModelIdentifier().contains("openai")) { // 简陋的判断
        if(true){
            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .apiKey(modelConfig.getApiKey())
                    .modelName(modelConfig.getModelIdentifier())
                    .timeout(Duration.ofSeconds(60)); // 设置超时

            if (StringUtils.hasText(modelConfig.getUrlBase()) && !modelConfig.getUrlBase().contains("api.openai.com")) {
                builder.baseUrl(modelConfig.getUrlBase()); // 支持自定义 OpenAI 兼容的 Base URL
            }
            // 可以从 ChatRequest.options 中读取 temperature, maxTokens 等参数设置给 builder
            return builder.build();
        }
        else {
            throw new UnsupportedOperationException("不支持的模型提供商: " + modelConfig.getModelIdentifier());
        }
    }


    private ImageModel buildImageModel(LlmModel modelConfig, ImageGenerationRequest imageRequest) {
        if (modelConfig.getModelIdentifier().startsWith("dall-e-") ||
                modelConfig.getModelIdentifier().contains("openai")) { // 简陋的判断
            OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                    .apiKey(modelConfig.getApiKey())
                    .modelName(modelConfig.getModelIdentifier()) // e.g., "dall-e-3" or "dall-e-2"
                    .timeout(Duration.ofSeconds(120));

            if (StringUtils.hasText(modelConfig.getUrlBase()) && !modelConfig.getUrlBase().contains("api.openai.com")) {
                builder.baseUrl(modelConfig.getUrlBase());
            }
            if (StringUtils.hasText(imageRequest.getSize())) {
                builder.size(imageRequest.getSize());
            }
            if (StringUtils.hasText(imageRequest.getQuality())) {
                builder.quality(imageRequest.getQuality());
            }
            if (StringUtils.hasText(imageRequest.getStyle())) {
                builder.style(imageRequest.getStyle());
            }
            // builder.responseFormat("url"); // 或 "b64_json"
            // builder.user("user-id-from-request"); // 可选

            return builder.build();
        }
        else {
            throw new UnsupportedOperationException("不支持的图像模型提供商: " + modelConfig.getModelIdentifier());
        }
    }
}