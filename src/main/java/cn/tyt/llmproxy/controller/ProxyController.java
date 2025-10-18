package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ChatResponse_dto;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import cn.tyt.llmproxy.filter.AccessKeyInterceptor;
import cn.tyt.llmproxy.service.ILangchainProxyService;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class ProxyController {

    @Autowired
    private ILangchainProxyService langchainProxyService;

    @PostMapping("/chat")
    public Result<ChatResponse_dto> chat(@Valid @RequestBody ChatRequest_dto request, @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId, @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        ChatResponse_dto response = langchainProxyService.chat(request, userId, accessKeyId);
        return Result.success(response);
    }

    @PostMapping("/generate-image")
    public Result<ImageGenerationResponse> generateImage(@Valid @RequestBody ImageGenerationRequest request, @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId, @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        ImageGenerationResponse response = langchainProxyService.generateImage(request, userId, accessKeyId);
        return Result.success(response);
    }

    // TODO:实现异步任务接口并引入rabbitMQ

    // TODO: 实现流式聊天接口
    // @PostMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
    //     SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 设置超时时间
    //     try {
    //         langchainProxyService.streamChat(request, new StreamingResponseHandler<AiMessage>() {
    //             @Override
    //             public void onNext(String token) {
    //                 try {
    //                     emitter.send(SseEmitter.event().data(token));
    //                 } catch (IOException e) {
    //                     emitter.completeWithError(e);
    //                 }
    //             }
    //             @Override
    //             public void onComplete(Response<AiMessage> response) {
    //                 // 可以发送一个结束标记或最终的元数据
    //                 try {
    //                    emitter.send(SseEmitter.event().name("COMPLETE").data("Stream finished"));
    //                 } catch (IOException e) {
    //                    // log error
    //                 }
    //                 emitter.complete();
    //             }
    //             @Override
    //             public void onError(Throwable error) {
    //                 emitter.completeWithError(error);
    //             }
    //         });
    //     } catch (Exception e) {
    //         emitter.completeWithError(e);
    //     }
    //     return emitter;
    // }
}