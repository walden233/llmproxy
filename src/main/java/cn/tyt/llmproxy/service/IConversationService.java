package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.response.ConversationMessageDto;
import cn.tyt.llmproxy.dto.response.ConversationSummaryDto;
import cn.tyt.llmproxy.entity.ConversationSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IConversationService {

    ConversationSession createConversation(Integer userId, Integer accessKeyId, String title);

    ConversationSession ensureConversation(Integer userId, Integer accessKeyId, String conversationId, String titleHint);

    List<ConversationSummaryDto> listRecent(Integer userId, int limit);

    List<ConversationMessageDto> listMessages(String conversationId, Integer userId, Integer limit, LocalDateTime before);

    ConversationSession updateConversation(String conversationId, Integer userId, String title, Boolean pinned);

    void deleteConversation(String conversationId, Integer userId);

    ConversationAppendResult appendChatMessages(
            String conversationId,
            Integer userId,
            Integer accessKeyId,
            String userContent,
            List<String> userImageUrls,
            String assistantContent,
            String modelIdentifier,
            Integer promptTokens,
            Integer completionTokens,
            BigDecimal cost);

    ConversationTail getRecentTail(String conversationId, Integer userId, int limit);

    record ConversationAppendResult(String conversationId, String assistantMessageId) { }

    record ConversationTail(String conversationId, List<ConversationMessageDto> messages) { }
}
