package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.CreateConversationRequest;
import cn.tyt.llmproxy.dto.request.UpdateConversationRequest;
import cn.tyt.llmproxy.dto.response.ConversationMessageDto;
import cn.tyt.llmproxy.dto.response.ConversationSummaryDto;
import cn.tyt.llmproxy.entity.ConversationSession;
import cn.tyt.llmproxy.filter.AccessKeyInterceptor;
import cn.tyt.llmproxy.service.IConversationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/conversations")
public class ConversationController {

    @Autowired
    private IConversationService conversationService;

    @PostMapping
    public Result<ConversationSummaryDto> createConversation(@RequestBody(required = false) CreateConversationRequest request,
                                                             @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                                             @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        ConversationSession session = conversationService.createConversation(userId, accessKeyId, request == null ? null : request.getTitle());
        ConversationSummaryDto dto = new ConversationSummaryDto();
        dto.setConversationId(session.getConversationId());
        dto.setTitle(session.getTitle());
        dto.setPinned(session.getPinned());
        dto.setLastModelIdentifier(session.getLastModelIdentifier());
        dto.setLastMessageSummary(session.getLastMessageSummary());
        dto.setMessageCount(session.getMessageCount());
        dto.setLastActiveAt(session.getLastActiveAt());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        return Result.success(dto);
    }

    @GetMapping("/recent")
    public Result<List<ConversationSummaryDto>> recent(@RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                                       @RequestParam(value = "limit", defaultValue = "20") @Min(1) @Max(100) int limit) {
        List<ConversationSummaryDto> list = conversationService.listRecent(userId, limit);
        return Result.success(list);
    }

    @GetMapping("/{conversationId}/messages")
    public Result<List<ConversationMessageDto>> messages(@PathVariable String conversationId,
                                                         @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                                         @RequestParam(value = "limit", defaultValue = "50") @Min(1) @Max(200) int limit,
                                                         @RequestParam(value = "before", required = false) String before) {
        LocalDateTime beforeTime = before == null ? null : LocalDateTime.parse(before);
        List<ConversationMessageDto> messages = conversationService.listMessages(conversationId, userId, limit, beforeTime);
        return Result.success(messages);
    }

    @PatchMapping("/{conversationId}")
    public Result<ConversationSummaryDto> update(@PathVariable String conversationId,
                                                 @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                                 @RequestBody UpdateConversationRequest request) {
        ConversationSession session = conversationService.updateConversation(conversationId, userId, request.getTitle(), request.getPinned());
        ConversationSummaryDto dto = new ConversationSummaryDto();
        dto.setConversationId(session.getConversationId());
        dto.setTitle(session.getTitle());
        dto.setPinned(session.getPinned());
        dto.setLastModelIdentifier(session.getLastModelIdentifier());
        dto.setLastMessageSummary(session.getLastMessageSummary());
        dto.setMessageCount(session.getMessageCount());
        dto.setLastActiveAt(session.getLastActiveAt());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        return Result.success(dto);
    }

    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@PathVariable String conversationId,
                               @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId) {
        conversationService.deleteConversation(conversationId, userId);
        return Result.success();
    }
}
