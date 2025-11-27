package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.dto.openai.OpenAiChatRequest;
import cn.tyt.llmproxy.dto.openai.OpenAiChatResponse;
import cn.tyt.llmproxy.filter.AccessKeyInterceptor;
import cn.tyt.llmproxy.service.ILangchainProxyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v2")
public class OpenAIProxyController {

    @Autowired
    private ILangchainProxyService langchainProxyService;

    @PostMapping("/chat")
    public OpenAiChatResponse chatV2(@Valid @RequestBody OpenAiChatRequest request,
                                     @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                     @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        return langchainProxyService.chatV2(request, userId, accessKeyId, false);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatV2Stream(@Valid @RequestBody OpenAiChatRequest request,
                                   @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                   @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        return langchainProxyService.chatV2Stream(request, userId, accessKeyId, false);
    }

}
