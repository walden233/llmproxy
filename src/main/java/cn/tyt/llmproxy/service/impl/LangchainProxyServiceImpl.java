package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.common.enums.ModelCapabilityEnum;
import cn.tyt.llmproxy.common.enums.StatusEnum;
import cn.tyt.llmproxy.common.utils.ImgGeneration;
import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ChatResponse_dto;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import cn.tyt.llmproxy.entity.LlmModel;
import cn.tyt.llmproxy.mapper.LlmModelMapper;
import cn.tyt.llmproxy.service.ILangchainProxyService;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LangchainProxyServiceImpl implements ILangchainProxyService {

    private static final Logger log = LoggerFactory.getLogger(LangchainProxyServiceImpl.class);

    @Autowired
    private LlmModelMapper llmModelMapper;

//    // 简单内存会话存储，生产环境可能需要 Redis 或其他持久化存储
//    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();

    @Override
    public ChatResponse_dto chat(ChatRequest_dto request) {
        if(request.getImages()==null && request.getUserMessage()==null){
            throw new IllegalArgumentException("请求中必须包含图片或用户消息");
        }

        // 如果有图片，需要 'image-to-text' 能力；否则 'text-to-text'
        final String requiredCapability = (request.getImages() != null && !request.getImages().isEmpty())
                ? ModelCapabilityEnum.IMAGE_TO_TEXT.getValue()
                : ModelCapabilityEnum.TEXT_TO_TEXT.getValue();
        LlmModel selectedModel = selectModel(request.getModelInternalId(), request.getModelIdentifier(), requiredCapability);
        log.info("使用模型进行聊天: {} (ID: {}), 所需能力: {}", selectedModel.getDisplayName(), selectedModel.getModelIdentifier(), requiredCapability);

        OpenAiChatModel chatModel = buildChatLanguageModel(selectedModel, request.getOptions());

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
                    else if("system".equalsIgnoreCase(parts[0].trim())){
                        messages.add(SystemMessage.from(parts[1].trim()));
                    }
                }
            }
        }

        List<Content> contents=new ArrayList<>();
        //构建当前用户的消息
        UserMessage currentUserMessage;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // 多模态消息：包含文本和图片
            // 将 DTO 中的 ImageInput 转换为 langchain4j 的 Image 对象，然后转为ImageContent
            request.getImages().stream()
                    .map(imgInput -> {
                        if (StringUtils.hasText(imgInput.getBase64())) {
                            // 假设 base64 字符串不包含 "data:image/jpeg;base64," 前缀
                            // 如果包含，需要先去除
                            return new ImageContent(Image.builder()
                                    .base64Data(imgInput.getBase64())
                                    .mimeType(detectMimeType(imgInput.getBase64())) // 可选但推荐，或让模型自动识别
                                    .build());
                            //return new ImageContent(Image.builder().url(imgInput.getBase64()).build());
                        } else if (StringUtils.hasText(imgInput.getUrl())) {
                            return new ImageContent(Image.builder().url(imgInput.getUrl()).build());
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .forEach(contents::add);

            // 如果用户消息为空，也需要处理，只发送图片
            String userText = request.getUserMessage() == null ? "" : request.getUserMessage();
            contents.add(new TextContent(userText));
            currentUserMessage = UserMessage.from(contents);

        } else {
            // 纯文本消息
            currentUserMessage = UserMessage.from(request.getUserMessage());
        }
        messages.add(currentUserMessage);


        // 调用第三方大模型
        ChatResponse response = chatModel.chat(messages);

        if (response == null || response.aiMessage() == null) {
            throw new RuntimeException("模型未能生成响应。");
        }

        return new ChatResponse_dto(response.aiMessage().text(), selectedModel.getModelIdentifier());
    }


    //暂未使用
    @Override
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        String requiredCapability = (request.getOriginImage() != null)
                ? ModelCapabilityEnum.IMAGE_TO_IMAGE.getValue()
                : ModelCapabilityEnum.TEXT_TO_IMAGE.getValue();
        LlmModel selectedModel = selectModel(request.getModelInternalId(), request.getModelIdentifier(), requiredCapability);
        log.info("使用模型生成图像: {} (ID: {})", selectedModel.getDisplayName(), selectedModel.getModelIdentifier());
        ImageSynthesisResult result;
        try {
            if(requiredCapability.equals(ModelCapabilityEnum.TEXT_TO_IMAGE.getValue()))
                result = ImgGeneration.imageGenerate(request.getPrompt(), selectedModel.getApiKey(), selectedModel.getModelIdentifier(), request.getSize());
            else
                result = ImgGeneration.imageEdit(request.getPrompt(), selectedModel.getApiKey(), selectedModel.getModelIdentifier(), request.getSize(),
                        request.getOriginImage().getUrl());
        } catch (ApiException | NoApiKeyException e) {
            throw new RuntimeException(e.getMessage());
        }
        if(result.getOutput()==null || result.getOutput().getResults()==null){
            throw new RuntimeException("远端图片生成失败");
        }
        return new ImageGenerationResponse(
                result.getOutput().getResults().get(0).get("url"),
                result.getOutput().getResults().get(0).getOrDefault("actual_prompt",request.getPrompt()),
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

    /**
     * (辅助方法) 简单地从 Base64 字符串中检测 MIME 类型。
     * 实际应用中可能需要更可靠的库或方法。
     * @param base64Data Base64 编码的图片数据
     * @return 图片的 MIME 类型，如 "image/png", "image/jpeg"
     */
    private String detectMimeType(String base64Data) {
        // 这是一个非常简化的实现，仅用于示例
        // 真实场景下，前端最好能直接提供 mimeType
        // 或者后端使用更复杂的库来检测
        if (base64Data.startsWith("/9j/")) {
            return "image/jpeg";
        } else if (base64Data.startsWith("iVBORw0KGgo=")) {
            return "image/png";
        } else if (base64Data.startsWith("R0lGODdh")) {
            return "image/gif";
        }
        // 默认或未知
        return "application/octet-stream";
    }

    private OpenAiChatModel buildChatLanguageModel(LlmModel modelConfig, Map<String, Object> options) {

        // 目前只支持 OpenAI 兼容的模型，未来可以在这里扩展
        // if (isClaudeModel(modelConfig.getModelIdentifier())) { ... }

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(modelConfig.getApiKey())
                .modelName(modelConfig.getModelIdentifier())
                .timeout(Duration.ofSeconds(120)); // 超时时间

        // 支持自定义 OpenAI 兼容的 Base URL
        if (StringUtils.hasText(modelConfig.getUrlBase()) && !modelConfig.getUrlBase().contains("api.openai.com")) {
            builder.baseUrl(modelConfig.getUrlBase());
        }

        // 从 ChatRequest.options 中读取并设置模型参数
        if (options != null && !options.isEmpty()) {
            if (options.containsKey("temperature")) {
                builder.temperature(((Number) options.get("temperature")).doubleValue());
            }
            if (options.containsKey("maxTokens")) {
                builder.maxTokens(((Number) options.get("maxTokens")).intValue());
            }
            if (options.containsKey("topP")) {
                builder.topP(((Number) options.get("topP")).doubleValue());
            }
        }

        return builder.build();
    }


//    private ImageModel buildImageModel(LlmModel modelConfig, ImageGenerationRequest imageRequest) {
//        if (modelConfig.getModelIdentifier().startsWith("dall-e-") ||
//                modelConfig.getModelIdentifier().contains("openai")) { // 简陋的判断
//            OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
//                    .apiKey(modelConfig.getApiKey())
//                    .modelName(modelConfig.getModelIdentifier()) // e.g., "dall-e-3" or "dall-e-2"
//                    .timeout(Duration.ofSeconds(120));
//
//            if (StringUtils.hasText(modelConfig.getUrlBase()) && !modelConfig.getUrlBase().contains("api.openai.com")) {
//                builder.baseUrl(modelConfig.getUrlBase());
//            }
//            if (StringUtils.hasText(imageRequest.getSize())) {
//                builder.size(imageRequest.getSize());
//            }
//            if (StringUtils.hasText(imageRequest.getQuality())) {
//                builder.quality(imageRequest.getQuality());
//            }
//            if (StringUtils.hasText(imageRequest.getStyle())) {
//                builder.style(imageRequest.getStyle());
//            }
//            // builder.responseFormat("url"); // 或 "b64_json"
//            // builder.user("user-id-from-request"); // 可选
//
//            return builder.build();
//        }
//        else {
//            throw new UnsupportedOperationException("不支持的图像模型提供商: " + modelConfig.getModelIdentifier());
//        }
//    }
}