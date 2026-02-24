package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.common.enums.ModelCapabilityEnum;
import cn.tyt.llmproxy.common.enums.ResultCode;
import cn.tyt.llmproxy.common.enums.StatusEnum;
import cn.tyt.llmproxy.common.exception.BusinessException;
import cn.tyt.llmproxy.context.ModelUsageContext;
import cn.tyt.llmproxy.dto.LlmModelConfigDto;
import cn.tyt.llmproxy.dto.openai.OpenAiChatRequest;
import cn.tyt.llmproxy.dto.openai.OpenAiChatResponse;
import cn.tyt.llmproxy.dto.openai.OpenAiFunction;
import cn.tyt.llmproxy.dto.openai.OpenAiFunctionCall;
import cn.tyt.llmproxy.dto.openai.OpenAiMessage;
import cn.tyt.llmproxy.dto.openai.OpenAiMessageContent;
import cn.tyt.llmproxy.dto.openai.OpenAiResponseFormat;
import cn.tyt.llmproxy.dto.openai.OpenAiTool;
import cn.tyt.llmproxy.dto.openai.OpenAiToolCall;
import cn.tyt.llmproxy.entity.LlmModel;
import cn.tyt.llmproxy.entity.Provider;
import cn.tyt.llmproxy.entity.ProviderKey;
import cn.tyt.llmproxy.image.ImageGeneratorFactory;
import cn.tyt.llmproxy.image.ImageGeneratorService;
import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ConversationMessageDto;
import cn.tyt.llmproxy.dto.response.ChatResponse_dto;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import cn.tyt.llmproxy.mapper.LlmModelMapper;
import cn.tyt.llmproxy.mapper.ProviderMapper;
import cn.tyt.llmproxy.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonReferenceSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.math.BigDecimal;
import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class LangchainProxyServiceImpl implements ILangchainProxyService {

    private static final Logger log = LoggerFactory.getLogger(LangchainProxyServiceImpl.class);

    @Autowired
    private LlmModelMapper llmModelMapper;
    @Autowired
    private ProviderMapper providerMapper;
    @Autowired
    private KeySelectionService keySelectionService; // 注入新服务
    @Autowired
    private IUserService userService;
    @Autowired
    private IStatisticsService statisticsService;
    @Autowired
    private IProviderService providerService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IConversationService conversationService;

//    // 简单内存会话存储，生产环境可能需要 Redis 或其他持久化存储
//    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();

    @Override
    public ChatResponse_dto chat(ChatRequest_dto request, Integer userId, Integer accessKeyId, Boolean isAsync) {
        if(request.getImages()==null && request.getUserMessage()==null){
            throw new IllegalArgumentException("请求中必须包含图片或用户消息");
        }
        boolean persistHistory = Boolean.TRUE.equals(request.getPersistHistory());
        String conversationId = request.getConversationId();

        // 如果有图片，需要 'image-to-text' 能力；否则 'text-to-text'
        final String requiredCapability = (request.getImages() != null && !request.getImages().isEmpty())
                ? ModelCapabilityEnum.IMAGE_TO_TEXT.getValue()
                : ModelCapabilityEnum.TEXT_TO_TEXT.getValue();
        LlmModel selectedModel = selectModel(request.getModelInternalId(), request.getModelIdentifier(), requiredCapability);
        LlmModelConfigDto modelConfig = buildModelConfig(selectedModel);
        log.info("使用模型进行聊天: {} (ID: {}), 所需能力: {}", modelConfig.getDisplayName(), modelConfig.getModelIdentifier(), requiredCapability);
        ModelUsageContext.set(modelConfig.getId(), modelConfig.getModelIdentifier());
        OpenAiChatModel chatModel = buildChatLanguageModel(modelConfig, request.getOptions());
        List<ChatMessage> messages = new ArrayList<>();

        conversationId = ensureConversationIfNeeded(persistHistory, conversationId, userId, accessKeyId, request.getUserMessage());

        // 将最近的持久化消息拼接为上下文（仅在未显式传历史时使用）
        if (persistHistory && (request.getHistory() == null || request.getHistory().isEmpty())) {
            appendTailMessages(messages, conversationId, userId);
        }
        // 添加系统消息 (如果需要)
        // messages.add(SystemMessage.from("You are a helpful assistant."));

        // 处理历史消息
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            for (Map<String,String> historyMsg : request.getHistory()) {
                if (historyMsg.get("role").equals("user")) {
                    messages.add(UserMessage.from(historyMsg.get("content")));
                } else if (historyMsg.get("role").equals("assistant")) {
                    messages.add(AiMessage.from(historyMsg.get("content")));
                }
                else if(historyMsg.get("role").equals("system")){
                    messages.add(SystemMessage.from(historyMsg.get("content")));
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
//                            return new ImageContent(Image.builder().url(imgInput.getBase64()).build());
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

        ChatRequest chatRequest = buildChatRequestFromOptions(messages, request.getOptions());
        ChatResponse response = executeChatRequest(chatModel, chatRequest, modelConfig);

        if (response == null || response.aiMessage() == null) {
            throw new BusinessException(ResultCode.MODEL_INFERENCE_ERROR, "模型未能生成响应。");
        }
        BigDecimal cost = calculateChatPrice(response,modelConfig);
        //使用工作队列处理扣费和记录？
        userService.creditUserBalance(userId,cost);

        int inputTokensCount = response.metadata().tokenUsage().inputTokenCount();
        int outputTokensCount = response.metadata().tokenUsage().outputTokenCount();
        statisticsService.recordUsageMongo(userId,accessKeyId,modelConfig.getId(),inputTokensCount,outputTokensCount,null,cost.negate(), LocalDateTime.now(),true,isAsync);
        String assistantContent = response.aiMessage().text();
        String messageId = null;
        if (persistHistory) {
            List<String> imageUrls = request.getImages() == null ? Collections.emptyList() :
                    request.getImages().stream()
                            .map(img -> StringUtils.hasText(img.getUrl()) ? img.getUrl() : img.getBase64())
                            .filter(StringUtils::hasText)
                            .collect(Collectors.toList());
            IConversationService.ConversationAppendResult appendResult = conversationService.appendChatMessages(
                    conversationId,
                    userId,
                    accessKeyId,
                    request.getUserMessage(),
                    imageUrls,
                    assistantContent,
                    modelConfig.getModelIdentifier(),
                    inputTokensCount,
                    outputTokensCount,
                    cost
            );
            conversationId = appendResult.conversationId();
            messageId = appendResult.assistantMessageId();
        }
        return new ChatResponse_dto(assistantContent, modelConfig.getModelIdentifier(),inputTokensCount,outputTokensCount, conversationId, messageId);
    }

    @Override
    public OpenAiChatResponse chatV2(OpenAiChatRequest request, Integer userId, Integer accessKeyId, Boolean isAsync) {
        validateOpenAiRequest(request, false);
        boolean persistHistory = Boolean.TRUE.equals(request.getPersistHistory());
        String conversationId = request.getConversationId();
        boolean containsImageInput = containsImageInputs(request);
        final String requiredCapability = containsImageInput
                ? ModelCapabilityEnum.IMAGE_TO_TEXT.getValue()
                : ModelCapabilityEnum.TEXT_TO_TEXT.getValue();
        LlmModel selectedModel = selectModel(null, request.getModel(), requiredCapability);
        LlmModelConfigDto modelConfig = buildModelConfig(selectedModel);
        log.info("v2/chat 使用模型: {} (ID: {}), 所需能力: {}", modelConfig.getDisplayName(), modelConfig.getModelIdentifier(), requiredCapability);
        ModelUsageContext.set(modelConfig.getId(), modelConfig.getModelIdentifier());
        OpenAiChatModel chatModel = buildChatLanguageModel(modelConfig, null);

        conversationId = ensureConversationIfNeeded(persistHistory, conversationId, userId, accessKeyId, extractLastUserMessage(request));

        List<ChatMessage> messages = new ArrayList<>();
        if (persistHistory) {
            appendTailMessages(messages, conversationId, userId);
        }
        messages.addAll(convertOpenAiMessages(request));
        ChatRequest chatRequest = buildChatRequest(request, messages, modelConfig.getModelIdentifier());
        ChatResponse response = executeChatRequest(chatModel, chatRequest, modelConfig);

        if (response == null || response.aiMessage() == null) {
            throw new BusinessException(ResultCode.MODEL_INFERENCE_ERROR, "模型未能生成响应。");
        }
        BigDecimal cost = calculateChatPrice(response,modelConfig);
        userService.creditUserBalance(userId,cost);

        TokenUsage usage = response.metadata() != null ? response.metadata().tokenUsage() : response.tokenUsage();
        Integer inputTokensCount = usage != null ? usage.inputTokenCount() : null;
        Integer outputTokensCount = usage != null ? usage.outputTokenCount() : null;
        statisticsService.recordUsageMongo(userId,accessKeyId,modelConfig.getId(),inputTokensCount,outputTokensCount,null,cost.negate(), LocalDateTime.now(),true,isAsync);
        String assistantContent = response.aiMessage().text();
        if (persistHistory) {
            List<String> userImageUrls = extractUserImageUrls(request);
            IConversationService.ConversationAppendResult appendResult = conversationService.appendChatMessages(
                    conversationId,
                    userId,
                    accessKeyId,
                    extractLastUserMessage(request),
                    userImageUrls,
                    assistantContent,
                    modelConfig.getModelIdentifier(),
                    inputTokensCount,
                    outputTokensCount,
                    cost
            );
            conversationId = appendResult.conversationId();
        }
        OpenAiChatResponse chatResponse = buildOpenAiChatResponse(response, modelConfig);
        chatResponse.setConversationId(conversationId);
        return chatResponse;
    }

    @Override
    public SseEmitter chatV2Stream(OpenAiChatRequest request, Integer userId, Integer accessKeyId, Boolean isAsync) {
        validateOpenAiRequest(request, true);
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                boolean persistHistory = Boolean.TRUE.equals(request.getPersistHistory());
                final String[] conversationId = {request.getConversationId()};
                boolean containsImageInput = containsImageInputs(request);
                final String requiredCapability = containsImageInput
                        ? ModelCapabilityEnum.IMAGE_TO_TEXT.getValue()
                        : ModelCapabilityEnum.TEXT_TO_TEXT.getValue();
                LlmModel selectedModel = selectModel(null, request.getModel(), requiredCapability);
                LlmModelConfigDto modelConfig = buildModelConfig(selectedModel);
                log.info("v2/chat/stream 使用模型: {} (ID: {}), 所需能力: {}", modelConfig.getDisplayName(), modelConfig.getModelIdentifier(), requiredCapability);
                ModelUsageContext.set(modelConfig.getId(), modelConfig.getModelIdentifier());
                OpenAiStreamingChatModel streamingModel = buildStreamingChatLanguageModel(modelConfig);
                conversationId[0] = ensureConversationIfNeeded(persistHistory, conversationId[0], userId, accessKeyId, extractLastUserMessage(request));

                List<ChatMessage> messages = new ArrayList<>();
                if (persistHistory) {
                    appendTailMessages(messages, conversationId[0], userId);
                }
                messages.addAll(convertOpenAiMessages(request));
                ChatRequest chatRequest = buildChatRequest(request, messages, modelConfig.getModelIdentifier());
                final String streamId = "chatcmpl-" + UUID.randomUUID();
                final long created = Instant.now().getEpochSecond();
                AtomicBoolean roleSignaled = new AtomicBoolean(false);
                StringBuilder assistantBuilder = new StringBuilder();
                sendInitialStreamChunk(emitter, streamId, created, modelConfig.getModelIdentifier(), roleSignaled);
                StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        if (!StringUtils.hasText(partialResponse)) {
                            return;
                        }
                        assistantBuilder.append(partialResponse);
                        Map<String, Object> payload = buildStreamChunk(streamId, created, modelConfig.getModelIdentifier(), partialResponse, roleSignaled.compareAndSet(false, true));
                        if (payload != null) {
                            sendStreamData(emitter, payload);
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        try {
                            BigDecimal cost = calculateChatPrice(response, modelConfig);
                            userService.creditUserBalance(userId, cost);
                            TokenUsage usage = response.metadata() != null ? response.metadata().tokenUsage() : response.tokenUsage();
                            Integer inputTokensCount = usage != null ? usage.inputTokenCount() : null;
                            Integer outputTokensCount = usage != null ? usage.outputTokenCount() : null;
                            statisticsService.recordUsageMongo(userId, accessKeyId, modelConfig.getId(), inputTokensCount, outputTokensCount, null, cost.negate(), LocalDateTime.now(), true, isAsync);

                            if (persistHistory) {
                                List<String> userImageUrls = extractUserImageUrls(request);
                                IConversationService.ConversationAppendResult appendResult = conversationService.appendChatMessages(
                                        conversationId[0],
                                        userId,
                                        accessKeyId,
                                        extractLastUserMessage(request),
                                        userImageUrls,
                                        assistantBuilder.toString(),
                                        modelConfig.getModelIdentifier(),
                                        inputTokensCount,
                                        outputTokensCount,
                                        cost
                                );
                                conversationId[0] = appendResult.conversationId();
                            }

                            List<OpenAiToolCall> toolCalls = buildResponseToolCalls(response.aiMessage());
                            String finishReason = resolveFinishReason(response, toolCalls);
                            Map<String, Object> finalChunk = buildTerminalStreamChunk(streamId, created, modelConfig.getModelIdentifier(), toolCalls, finishReason);
                            if (persistHistory && StringUtils.hasText(conversationId[0]) && finalChunk != null) {
                                finalChunk.put("conversation_id", conversationId[0]);
                            }
                            if (finalChunk != null) {
                                sendStreamData(emitter, finalChunk);
                            }
                            sendStreamDone(emitter);
                            emitter.complete();
                        } catch (Exception e) {
                            handleStreamError(emitter, e);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        handleStreamError(emitter, error);
                    }
                };
                try {
                    streamingModel.doChat(chatRequest, handler);
                } catch (AuthenticationException e) {
                    providerService.updateKeyStatus(modelConfig.getProviderKeyId(), false);
                    throw e;
                } catch (RateLimitException e) {
                    keySelectionService.reportKeyFailure(modelConfig.getProviderKeyId(), 5 * 60);
                    throw e;
                }
            } catch (Throwable t) {
                handleStreamError(emitter, t);
            } finally {
                ModelUsageContext.clear();
            }
        });
        return emitter;
    }

    private BigDecimal calculateChatPrice(ChatResponse response,LlmModelConfigDto modelConfig){
        int inputTokensCount = response.metadata().tokenUsage().inputTokenCount();
        int outputTokensCount = response.metadata().tokenUsage().outputTokenCount();
        Map<String,Object> pricing = modelConfig.getPricing();

        BigDecimal inputPrice = new BigDecimal(pricing.get("input").toString());
        BigDecimal outputPrice = new BigDecimal(pricing.get("output").toString());

        BigDecimal inputTokens = new BigDecimal(inputTokensCount);
        BigDecimal outputTokens = new BigDecimal(outputTokensCount);

        BigDecimal inputCost = inputTokens.multiply(inputPrice);
        BigDecimal outputCost = outputTokens.multiply(outputPrice);

        return inputCost.add(outputCost).negate();
    }


    @Override
    public ImageGenerationResponse generateImage(ImageGenerationRequest request, Integer userId, Integer accessKeyId, Boolean isAsync) {
        String requiredCapability = (request.getOriginImage() != null)
                ? ModelCapabilityEnum.IMAGE_TO_IMAGE.getValue()
                : ModelCapabilityEnum.TEXT_TO_IMAGE.getValue();
        LlmModel selectedModel = selectModel(request.getModelInternalId(), request.getModelIdentifier(), requiredCapability);
        LlmModelConfigDto modelConfig = buildModelConfig(selectedModel);
        log.info("使用模型生成图像: {} (ID: {})", modelConfig.getDisplayName(), modelConfig.getModelIdentifier());
        ModelUsageContext.set(modelConfig.getId(), modelConfig.getModelIdentifier());
        ImageGenerationResponse result;
        ImageGeneratorService generator = ImageGeneratorFactory.createGenerator(modelConfig);
        if(requiredCapability.equals(ModelCapabilityEnum.TEXT_TO_IMAGE.getValue()))
            result = generator.generateImage(request);
        else
            result = generator.editImage(request);
        BigDecimal cost = calculateImagePrice(result,modelConfig);
        userService.creditUserBalance(userId,cost);
        statisticsService.recordUsageMongo(userId,accessKeyId,modelConfig.getId(),null,null,result.getImageUrls().size(),cost.negate(), LocalDateTime.now(),true,isAsync);
        return result;
    }
    private BigDecimal calculateImagePrice(ImageGenerationResponse response,LlmModelConfigDto modelConfig){
        int imgCount = response.getImageUrls().size();
        Map<String,Object> pricing = modelConfig.getPricing();
        BigDecimal imgPrice = new BigDecimal(pricing.get("output").toString());
        return imgPrice.multiply(new BigDecimal(imgCount)).negate();
    }

    private String ensureConversationIfNeeded(boolean persistHistory, String conversationId, Integer userId, Integer accessKeyId, String seedMessage) {
        if (!persistHistory) {
            return conversationId;
        }
        if (!StringUtils.hasText(conversationId)) {
            return conversationService.ensureConversation(userId, accessKeyId, null, seedMessage).getConversationId();
        }
        conversationService.ensureConversation(userId, accessKeyId, conversationId, seedMessage);
        return conversationId;
    }

    private void appendTailMessages(List<ChatMessage> messages, String conversationId, Integer userId) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        List<ConversationMessageDto> tailMessages = conversationService.getRecentTail(conversationId, userId, 10).messages();
        for (ConversationMessageDto msg : tailMessages) {
            if ("user".equals(msg.getRole())) {
                messages.add(UserMessage.from(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(AiMessage.from(msg.getContent()));
            } else if ("system".equals(msg.getRole())) {
                messages.add(SystemMessage.from(msg.getContent()));
            }
        }
    }

    private ChatRequest buildChatRequestFromOptions(List<ChatMessage> messages, Map<String, Object> options) {
        ChatRequest.Builder builder = ChatRequest.builder();
        if (options != null) {
            if (options.containsKey("temperature")) {
                builder.temperature(((Number) options.get("temperature")).doubleValue());
            }
            if (options.containsKey("max_tokens")) {
                builder.maxOutputTokens(((Number) options.get("max_tokens")).intValue());
            }
            if (options.containsKey("top_p")) {
                builder.topP(((Number) options.get("top_p")).doubleValue());
            }
            if (options.containsKey("frequency_penalty")) {
                builder.frequencyPenalty(((Number) options.get("frequency_penalty")).doubleValue());
            }
        }
        return builder.messages(messages).build();
    }

    private LlmModel selectModel(Integer internalId, String identifier, String requiredCapability) {
        Optional<LlmModel> modelOpt = Optional.empty();
        if (internalId!=null) {
            try {
                modelOpt = Optional.ofNullable(llmModelMapper.selectById(internalId));
            } catch (NumberFormatException e) {
                log.warn("无效的内部模型ID格式: {}", internalId);
            }
        } else if (StringUtils.hasText(identifier)) {
            modelOpt = Optional.ofNullable(llmModelMapper.selectOne(
                    new LambdaQueryWrapper<LlmModel>().eq(LlmModel::getModelIdentifier, identifier)
            ));
        }

        if (modelOpt.isPresent()) {
            LlmModel model = modelOpt.get();
            if (model.getStatus() != StatusEnum.AVAILABLE.getCode()) {
                throw new BusinessException(ResultCode.MODEL_OFFLINE, "模型 " + model.getDisplayName() + " 当前不可用。");
            }
            if (!model.getCapabilities().contains(requiredCapability)) {
                throw new BusinessException(ResultCode.MODEL_CONFIG_ERROR, "模型 " + model.getDisplayName() + " 不支持 " + requiredCapability + " 功能。");
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
                throw new BusinessException(ResultCode.MODEL_NOT_FOUND, "没有找到支持 " + requiredCapability + " 功能的可用模型。");
            }
            return availableModels.get(0); // 选择优先级最高的
        }
    }

    private LlmModelConfigDto buildModelConfig(LlmModel model) {
        Provider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null)
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "没有找到" + model.getModelIdentifier() + " 的提供商");

        ProviderKey providerKey = keySelectionService.selectAvailableKey(provider.getId());

        LlmModelConfigDto modelConfig = new LlmModelConfigDto();
        BeanUtils.copyProperties(model, modelConfig);
        modelConfig.setProviderName(provider.getName());
        modelConfig.setUrlBase(provider.getUrlBase());
        modelConfig.setApiKey(providerKey.getApiKey());
        modelConfig.setProviderKeyId(providerKey.getId());
        return modelConfig;
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
        } else if (base64Data.startsWith("iVBORw0KGgo")) {
            return "image/png";
        } else if (base64Data.startsWith("R0lGODdh")) {
            return "image/gif";
        }
        // 默认或未知
        return "application/octet-stream";
    }

    private OpenAiChatModel buildChatLanguageModel(LlmModelConfigDto modelConfig, Map<String, Object> options) {

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

    private OpenAiStreamingChatModel buildStreamingChatLanguageModel(LlmModelConfigDto modelConfig) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(modelConfig.getApiKey())
                .modelName(modelConfig.getModelIdentifier())
                .timeout(Duration.ofSeconds(120));

        if (StringUtils.hasText(modelConfig.getUrlBase()) && !modelConfig.getUrlBase().contains("api.openai.com")) {
            builder.baseUrl(modelConfig.getUrlBase());
        }
        return builder.build();
    }

    private ChatResponse executeChatRequest(OpenAiChatModel chatModel, ChatRequest chatRequest, LlmModelConfigDto modelConfig) {
        try {
            return chatModel.chat(chatRequest);
        } catch (AuthenticationException e) {
            providerService.updateKeyStatus(modelConfig.getProviderKeyId(), false);
            throw e;
        } catch (RateLimitException e) {
            keySelectionService.reportKeyFailure(modelConfig.getProviderKeyId(), 5 * 60);
            throw e;
        }
    }

    private void validateOpenAiRequest(OpenAiChatRequest request, boolean streaming) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "messages 不能为空");
        }
        if (!streaming && Boolean.TRUE.equals(request.getStream())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "stream=true 仅支持 /v2/chat/stream 接口");
        }
        if (streaming && !Boolean.TRUE.equals(request.getStream())) {
            request.setStream(true);
        }
        if (Boolean.TRUE.equals(request.getParallelToolCalls())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "暂不支持 parallel_tool_calls");
        }
    }

    private boolean containsImageInputs(OpenAiChatRequest request) {
        return request.getMessages().stream()
                .filter(Objects::nonNull)
                .filter(msg -> "user".equalsIgnoreCase(msg.getRole()))
                .anyMatch(msg -> hasImageContent(msg.getContents()));
    }

    private String extractLastUserMessage(OpenAiChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return null;
        }
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            OpenAiMessage message = request.getMessages().get(i);
            if (message != null && "user".equalsIgnoreCase(message.getRole())) {
                return joinTextContent(message.getContents());
            }
        }
        return null;
    }

    private String joinTextContent(List<OpenAiMessageContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return null;
        }
        return contents.stream()
                .filter(Objects::nonNull)
                .filter(c -> "text".equalsIgnoreCase(c.getType()))
                .map(OpenAiMessageContent::getText)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    private List<String> extractUserImageUrls(OpenAiChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return Collections.emptyList();
        }
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            OpenAiMessage message = request.getMessages().get(i);
            if (message == null || !"user".equalsIgnoreCase(message.getRole())) {
                continue;
            }
            if (message.getContents() == null) {
                continue;
            }
            return message.getContents().stream()
                    .filter(Objects::nonNull)
                    .filter(c -> "image_url".equalsIgnoreCase(c.getType()))
                    .map(OpenAiMessageContent::getImageUrl)
                    .filter(Objects::nonNull)
                    .map(img -> StringUtils.hasText(img.getUrl()) ? img.getUrl() : img.getBase64Json())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<ChatMessage> convertOpenAiMessages(OpenAiChatRequest request) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (OpenAiMessage message : request.getMessages()) {
            if (message == null || !StringUtils.hasText(message.getRole())) {
                continue;
            }
            String role = message.getRole().toLowerCase(Locale.ROOT);
            switch (role) {
                case "system" -> chatMessages.add(SystemMessage.from(aggregateText(message.getContents())));
                case "user" -> chatMessages.add(buildUserMessage(message));
                case "assistant" -> chatMessages.add(buildAssistantMessage(message));
                case "tool", "function" -> chatMessages.add(buildToolMessage(message));
                default -> throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的角色: " + role);
            }
        }
        return chatMessages;
    }

    private ChatMessage buildUserMessage(OpenAiMessage message) {
        List<Content> contentList = convertUserContents(message.getContents());
        return UserMessage.from(contentList);
    }

    private ChatMessage buildAssistantMessage(OpenAiMessage message) {
        List<ToolExecutionRequest> toolExecutionRequests = toToolExecutionRequests(message.getToolCalls());
        String text = aggregateText(message.getContents());
        if (!toolExecutionRequests.isEmpty()) {
            return AiMessage.from(text, toolExecutionRequests);
        }
        return AiMessage.from(text);
    }

    private ChatMessage buildToolMessage(OpenAiMessage message) {
        String toolCallId = message.getToolCallId();
        if (!StringUtils.hasText(toolCallId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "tool 消息缺少 tool_call_id");
        }
        String text = aggregateText(message.getContents());
        return ToolExecutionResultMessage.from(toolCallId, message.getName(), text);
    }

    private List<Content> convertUserContents(List<OpenAiMessageContent> contents) {
        List<Content> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(contents)) {
            result.add(new TextContent(""));
            return result;
        }
        for (OpenAiMessageContent content : contents) {
            if (content == null) {
                continue;
            }
            if ("image_url".equalsIgnoreCase(content.getType()) && content.getImageUrl() != null) {
                Image.Builder imageBuilder = Image.builder();
                if (StringUtils.hasText(content.getImageUrl().getUrl())) {
                    imageBuilder.url(content.getImageUrl().getUrl());
                }
                if (StringUtils.hasText(content.getImageUrl().getBase64Json())) {
                    imageBuilder.base64Data(content.getImageUrl().getBase64Json());
                    String mimeType = StringUtils.hasText(content.getImageUrl().getMimeType())
                            ? content.getImageUrl().getMimeType()
                            : detectMimeType(content.getImageUrl().getBase64Json());
                    imageBuilder.mimeType(mimeType);
                }
                result.add(new ImageContent(imageBuilder.build()));
            } else {
                result.add(new TextContent(content.getText() == null ? "" : content.getText()));
            }
        }
        return result;
    }

    private boolean hasImageContent(List<OpenAiMessageContent> contents) {
        if (CollectionUtils.isEmpty(contents)) {
            return false;
        }
        return contents.stream()
                .filter(Objects::nonNull)
                .anyMatch(content -> "image_url".equalsIgnoreCase(content.getType()) && content.getImageUrl() != null);
    }

    private String aggregateText(List<OpenAiMessageContent> contents) {
        if (CollectionUtils.isEmpty(contents)) {
            return "";
        }
        return contents.stream()
                .filter(Objects::nonNull)
                .map(OpenAiMessageContent::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    private List<ToolExecutionRequest> toToolExecutionRequests(List<OpenAiToolCall> toolCalls) {
        if (CollectionUtils.isEmpty(toolCalls)) {
            return Collections.emptyList();
        }
        if (toolCalls.size() > 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "暂不支持 parallel tool calls");
        }
        return toolCalls.stream()
                .map(this::buildToolExecutionRequest)
                .collect(Collectors.toList());
    }

    private ToolExecutionRequest buildToolExecutionRequest(OpenAiToolCall toolCall) {
        if (toolCall == null || toolCall.getFunction() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "tool 调用缺少 function 定义");
        }
        String id = StringUtils.hasText(toolCall.getId()) ? toolCall.getId() : UUID.randomUUID().toString();
        return ToolExecutionRequest.builder()
                .id(id)
                .name(toolCall.getFunction().getName())
                .arguments(serializeArguments(toolCall.getFunction().getArguments()))
                .build();
    }

    private String serializeArguments(Object arguments) {
        if (arguments == null) {
            return "{}";
        }
        if (arguments instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无法序列化 tool arguments");
        }
    }

    private void applyRequestParameters(ChatRequest.Builder builder, OpenAiChatRequest request) {
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        if (request.getTopP() != null) {
            builder.topP(request.getTopP());
        }
        if (request.getMaxTokens() != null) {
            builder.maxOutputTokens(request.getMaxTokens());
        }
        if (request.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(request.getFrequencyPenalty());
        }
        if (request.getPresencePenalty() != null) {
            builder.presencePenalty(request.getPresencePenalty());
        }
        if (!CollectionUtils.isEmpty(request.getStop())) {
            builder.stopSequences(request.getStop());
        }
    }

    private ChatRequest buildChatRequest(OpenAiChatRequest request, List<ChatMessage> messages, String modelName) {
        ChatRequest.Builder builder = ChatRequest.builder().messages(messages);
        if (StringUtils.hasText(modelName)) {
            builder.modelName(modelName);
        }
        applyRequestParameters(builder, request);
        configureResponseFormat(builder, request.getResponseFormat());
        configureTools(builder, request);
        ChatRequest chatRequest = builder.build();
        OpenAiChatRequestParameters parameters = buildOpenAiParameters(chatRequest, request);
        return chatRequest.toBuilder()
                .parameters(parameters)
                .build();
    }

    private OpenAiChatRequestParameters buildOpenAiParameters(ChatRequest chatRequest, OpenAiChatRequest request) {
        ChatRequestParameters rawParameters = chatRequest.parameters();
        OpenAiChatRequestParameters.Builder parametersBuilder = OpenAiChatRequestParameters.builder();
        if (rawParameters != null) {
            parametersBuilder.overrideWith(rawParameters);
        }
        applyOpenAiSpecificParameters(parametersBuilder, request);
        return parametersBuilder.build();
    }

    private void applyOpenAiSpecificParameters(OpenAiChatRequestParameters.Builder builder, OpenAiChatRequest request) {
        if (request.getParallelToolCalls() != null) {
            builder.parallelToolCalls(request.getParallelToolCalls());
        }
        Map<String, String> metadata = convertMetadata(request.getMetadata());
        if (!CollectionUtils.isEmpty(metadata)) {
            builder.metadata(metadata);
        }
    }

    private Map<String, String> convertMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        metadata.forEach((key, value) -> {
            if (StringUtils.hasText(key) && value != null) {
                result.put(key, value.toString());
            }
        });
        return result;
    }

    private void configureResponseFormat(ChatRequest.Builder builder, OpenAiResponseFormat responseFormat) {
        if (responseFormat == null || !StringUtils.hasText(responseFormat.getType())) {
            return;
        }
        String type = responseFormat.getType().toLowerCase(Locale.ROOT);
        switch (type) {
            case "json_object" -> builder.responseFormat(ResponseFormat.JSON);
            case "text" -> builder.responseFormat(ResponseFormat.TEXT);
            case "json_schema" -> {
                validateJsonSchema(responseFormat);
                JsonSchema jsonSchema = buildJsonSchema(responseFormat);
                builder.responseFormat(ResponseFormat.builder()
                        .type(ResponseFormatType.JSON)
                        .jsonSchema(jsonSchema)
                        .build());
            }
            default -> throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的 response_format: " + type);
        }
    }

    private void configureTools(ChatRequest.Builder builder, OpenAiChatRequest request) {
        if (CollectionUtils.isEmpty(request.getTools())) {
            return;
        }
        if (request.getToolChoice() != null && request.getToolChoice().isTextual()
                && "none".equalsIgnoreCase(request.getToolChoice().asText())) {
            return;
        }
        if (request.getToolChoice() != null && request.getToolChoice().getNodeType() == JsonNodeType.OBJECT) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "暂不支持 tool_choice.function 精确指定");
        }
        List<ToolSpecification> specifications = request.getTools().stream()
                .map(this::toToolSpecification)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (specifications.isEmpty()) {
            return;
        }
        builder.toolSpecifications(specifications);
        ToolChoice toolChoice = resolveToolChoice(request.getToolChoice());
        if (toolChoice != null) {
            builder.toolChoice(toolChoice);
        }
    }

    private ToolChoice resolveToolChoice(JsonNode toolChoiceNode) {
        if (toolChoiceNode == null || toolChoiceNode.isNull()) {
            return null;
        }
        if (toolChoiceNode.isTextual()) {
            String value = toolChoiceNode.asText("");
            if ("auto".equalsIgnoreCase(value)) {
                return ToolChoice.AUTO;
            }
            if ("required".equalsIgnoreCase(value)) {
                return ToolChoice.REQUIRED;
            }
            if ("none".equalsIgnoreCase(value)) {
                return null;
            }
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的 tool_choice: " + value);
        }
        return null;
    }

    private ToolSpecification toToolSpecification(OpenAiTool tool) {
        if (tool == null || !"function".equalsIgnoreCase(tool.getType())) {
            return null;
        }
        OpenAiFunction function = tool.getFunction();
        if (function == null || !StringUtils.hasText(function.getName())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "tool.function.name 不能为空");
        }
        JsonObjectSchema parameters = toJsonObjectSchema(function.getParameters());
        return ToolSpecification.builder()
                .name(function.getName())
                .description(function.getDescription())
                .parameters(parameters)
                .build();
    }

    @SuppressWarnings("unchecked")
    private JsonObjectSchema toJsonObjectSchema(Map<String, Object> schemaMap) {
        if (schemaMap == null || schemaMap.isEmpty()) {
            return JsonObjectSchema.builder().build();
        }
        JsonSchemaElement element = toJsonSchemaElement(schemaMap);
        if (element instanceof JsonObjectSchema objectSchema) {
            return objectSchema;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "tool parameters 必须是 type=object");
    }

    @SuppressWarnings("unchecked")
    private JsonSchemaElement toJsonSchemaElement(Object schemaObj) {
        if (!(schemaObj instanceof Map<?, ?> schemaMap)) {
            return JsonStringSchema.builder().build();
        }
        if (schemaMap.containsKey("$ref")) {
            return JsonReferenceSchema.builder().reference(String.valueOf(schemaMap.get("$ref"))).build();
        }
        String description = schemaMap.get("description") instanceof String ? (String) schemaMap.get("description") : null;
        Object typeObj = schemaMap.get("type");
        String type = typeObj != null ? typeObj.toString() : null;
        if ("object".equals(type) || (type == null && schemaMap.containsKey("properties"))) {
            JsonObjectSchema.Builder builder = JsonObjectSchema.builder().description(description);
            Map<String, Object> props = (Map<String, Object>) schemaMap.get("properties");
            if (props != null) {
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    JsonSchemaElement child = toJsonSchemaElement(entry.getValue());
                    if (child != null) {
                        builder.addProperty(entry.getKey(), child);
                    }
                }
            }
            Object requiredObj = schemaMap.get("required");
            List<String> required = toStringList(requiredObj);
            if (!required.isEmpty()) {
                builder.required(required);
            }
            Object additional = schemaMap.get("additionalProperties");
            if (additional instanceof Boolean bool) {
                builder.additionalProperties(bool);
            }
            Map<String, Object> definitions = (Map<String, Object>) schemaMap.get("definitions");
            if (definitions != null && !definitions.isEmpty()) {
                Map<String, JsonSchemaElement> converted = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : definitions.entrySet()) {
                    JsonSchemaElement def = toJsonSchemaElement(entry.getValue());
                    if (def != null) {
                        converted.put(entry.getKey(), def);
                    }
                }
                builder.definitions(converted);
            }
            return builder.build();
        }
        if ("array".equals(type)) {
            JsonArraySchema.Builder builder = JsonArraySchema.builder().description(description);
            Object items = schemaMap.get("items");
            if (items != null) {
                builder.items(toJsonSchemaElement(items));
            }
            return builder.build();
        }
        if ("integer".equals(type)) {
            return JsonIntegerSchema.builder().description(description).build();
        }
        if ("number".equals(type)) {
            return JsonNumberSchema.builder().description(description).build();
        }
        if ("boolean".equals(type)) {
            return JsonBooleanSchema.builder().description(description).build();
        }
        List<String> enumValues = toStringList(schemaMap.get("enum"));
        if (!enumValues.isEmpty()) {
            return JsonEnumSchema.builder().description(description).enumValues(enumValues).build();
        }
        Object anyOf = schemaMap.get("anyOf");
        List<JsonSchemaElement> anyOfElements = toSchemaElementList(anyOf);
        if (!anyOfElements.isEmpty()) {
            return JsonAnyOfSchema.builder().description(description).anyOf(anyOfElements).build();
        }
        return JsonStringSchema.builder().description(description).build();
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(Object::toString).collect(Collectors.toList());
    }

    private List<JsonSchemaElement> toSchemaElementList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<JsonSchemaElement> elements = new ArrayList<>();
        for (Object item : list) {
            JsonSchemaElement element = toJsonSchemaElement(item);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }

    private void validateJsonSchema(OpenAiResponseFormat responseFormat) {
        if (responseFormat.getJsonSchema() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "json_schema 未提供");
        }
        if (!StringUtils.hasText(responseFormat.getJsonSchema().getName())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "json_schema.name 不能为空");
        }
        if (responseFormat.getJsonSchema().getSchema() == null
                || responseFormat.getJsonSchema().getSchema().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "json_schema.schema 不能为空");
        }
    }

    private JsonSchema buildJsonSchema(OpenAiResponseFormat responseFormat) {
        if (responseFormat.getJsonSchema() == null) {
            return null;
        }
        JsonSchemaElement rootElement = toJsonSchemaElement(responseFormat.getJsonSchema().getSchema());
        return JsonSchema.builder()
                .name(responseFormat.getJsonSchema().getName())
                .rootElement(rootElement)
                .build();
    }

    private OpenAiChatResponse buildOpenAiChatResponse(ChatResponse response, LlmModelConfigDto modelConfig) {
        AiMessage aiMessage = response.aiMessage();
        List<OpenAiMessageContent> contents = buildResponseContents(aiMessage);
        List<OpenAiToolCall> toolCalls = buildResponseToolCalls(aiMessage);
        OpenAiChatResponse.ResponseMessage responseMessage = OpenAiChatResponse.ResponseMessage.builder()
                .role("assistant")
                .content(CollectionUtils.isEmpty(contents) ? null : contents)
                .toolCalls(CollectionUtils.isEmpty(toolCalls) ? null : toolCalls)
                .build();
        OpenAiChatResponse.Choice choice = OpenAiChatResponse.Choice.builder()
                .index(0)
                .message(responseMessage)
                .finishReason(resolveFinishReason(response, toolCalls))
                .build();
        TokenUsage usage = response.metadata() != null ? response.metadata().tokenUsage() : response.tokenUsage();
        OpenAiChatResponse.Usage usageDto = OpenAiChatResponse.Usage.builder()
                .promptTokens(usage != null ? usage.inputTokenCount() : null)
                .completionTokens(usage != null ? usage.outputTokenCount() : null)
                .totalTokens(usage != null ? usage.totalTokenCount() : null)
                .build();
        return OpenAiChatResponse.builder()
                .id(deriveResponseId(response))
                .object("chat.completion")
                .created(Instant.now().getEpochSecond())
                .model(modelConfig.getModelIdentifier())
                .choices(Collections.singletonList(choice))
                .usage(usageDto)
                .systemFingerprint(modelConfig.getProviderName())
                .build();
    }

    private List<OpenAiMessageContent> buildResponseContents(AiMessage aiMessage) {
        if (aiMessage == null || !StringUtils.hasText(aiMessage.text())) {
            return Collections.emptyList();
        }
        return Collections.singletonList(OpenAiMessageContent.textContent(aiMessage.text()));
    }

    private List<OpenAiToolCall> buildResponseToolCalls(AiMessage aiMessage) {
        if (aiMessage == null || CollectionUtils.isEmpty(aiMessage.toolExecutionRequests())) {
            return Collections.emptyList();
        }
        return aiMessage.toolExecutionRequests().stream()
                .map(req -> OpenAiToolCall.builder()
                        .id(req.id())
                        .type("function")
                        .function(OpenAiFunctionCall.builder()
                                .name(req.name())
                                .arguments(req.arguments())
                                .build())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveFinishReason(ChatResponse response, List<OpenAiToolCall> toolCalls) {
        FinishReason finishReason = response.finishReason();
        if (finishReason == null && response.metadata() != null) {
            finishReason = response.metadata().finishReason();
        }
        if (finishReason == null && !CollectionUtils.isEmpty(toolCalls)) {
            return "tool_calls";
        }
        if (finishReason == null) {
            return "stop";
        }
        return switch (finishReason) {
            case STOP -> "stop";
            case LENGTH -> "length";
            case TOOL_EXECUTION -> "tool_calls";
            case CONTENT_FILTER -> "content_filter";
            default -> "stop";
        };
    }

    private String deriveResponseId(ChatResponse response) {
        if (response.metadata() != null && StringUtils.hasText(response.metadata().id())) {
            return response.metadata().id();
        }
        if (StringUtils.hasText(response.id())) {
            return response.id();
        }
        return "chatcmpl-" + UUID.randomUUID();
    }

    private void sendInitialStreamChunk(SseEmitter emitter, String streamId, long created, String modelName, AtomicBoolean roleSignaled) {
        Map<String, Object> payload = buildStreamChunk(streamId, created, modelName, null, true);
        if (payload != null) {
            sendStreamData(emitter, payload);
            roleSignaled.set(true);
        }
    }

    private Map<String, Object> buildStreamChunk(String streamId, long created, String modelName, String text, boolean includeRole) {
        Map<String, Object> delta = new LinkedHashMap<>();
        if (includeRole) {
            delta.put("role", "assistant");
        }
        if (StringUtils.hasText(text)) {
            delta.put("content", Collections.singletonList(OpenAiMessageContent.textContent(text)));
        }
        if (delta.isEmpty()) {
            return null;
        }
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", null);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", streamId);
        payload.put("object", "chat.completion.chunk");
        payload.put("created", created);
        payload.put("model", modelName);
        payload.put("choices", Collections.singletonList(choice));
        return payload;
    }

    private Map<String, Object> buildTerminalStreamChunk(String streamId, long created, String modelName,
                                                         List<OpenAiToolCall> toolCalls, String finishReason) {
        Map<String, Object> delta = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(toolCalls)) {
            delta.put("tool_calls", toolCalls);
        }
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", finishReason);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", streamId);
        payload.put("object", "chat.completion.chunk");
        payload.put("created", created);
        payload.put("model", modelName);
        payload.put("choices", Collections.singletonList(choice));
        return payload;
    }

    private void sendStreamData(SseEmitter emitter, Object payload) {
        try {
            String data = (payload instanceof String str) ? str : objectMapper.writeValueAsString(payload);
            emitter.send(data);
        } catch (IOException e) {
            emitter.completeWithError(e);
            throw new RuntimeException("发送SSE数据失败", e);
        }
    }

    private void sendStreamDone(SseEmitter emitter) {
        try {
            emitter.send("[DONE]");
        } catch (IOException e) {
            emitter.completeWithError(e);
            throw new RuntimeException("发送SSE结束信号失败", e);
        }
    }

    private void handleStreamError(SseEmitter emitter, Throwable error) {
        if (emitter == null) {
            return;
        }
        log.error("Streaming chat error", error);
        Map<String, Object> errorPayload = new LinkedHashMap<>();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("message", error.getMessage());
        detail.put("type", error.getClass().getSimpleName());
        int code = error instanceof BusinessException businessException
                ? businessException.getCode()
                : ResultCode.INTERNAL_SERVER_ERROR.getCode();
        detail.put("code", code);
        errorPayload.put("error", detail);
        try {
            sendStreamData(emitter, errorPayload);
        } catch (RuntimeException ignored) {
            // connection likely closed
        }
        try {
            sendStreamDone(emitter);
        } catch (RuntimeException ignored) {
        }
        emitter.complete();
    }
}
