package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.common.enums.ResultCode;
import cn.tyt.llmproxy.common.exception.BusinessException;
import cn.tyt.llmproxy.document.ConversationMessageDocument;
import cn.tyt.llmproxy.dto.response.ConversationMessageDto;
import cn.tyt.llmproxy.dto.response.ConversationSummaryDto;
import cn.tyt.llmproxy.entity.ConversationSession;
import cn.tyt.llmproxy.mapper.ConversationSessionMapper;
import cn.tyt.llmproxy.repository.ConversationMessageRepository;
import cn.tyt.llmproxy.service.IConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements IConversationService {

    private final ConversationSessionMapper conversationSessionMapper;
    private final ConversationMessageRepository conversationMessageRepository;

    @Override
    public ConversationSession createConversation(Integer userId, Integer accessKeyId, String title) {
        ConversationSession session = new ConversationSession();
        session.setConversationId(generateConversationId());
        session.setUserId(userId);
        session.setAccessKeyId(accessKeyId);
        session.setTitle(StringUtils.hasText(title) ? title : "新会话");
        session.setPinned(false);
        session.setMessageCount(0);
        LocalDateTime now = LocalDateTime.now();
        session.setLastActiveAt(now);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setDeleted(false);
        conversationSessionMapper.insert(session);
        return session;
    }

    @Override
    public ConversationSession ensureConversation(Integer userId, Integer accessKeyId, String conversationId, String titleHint) {
        if (StringUtils.hasText(conversationId)) {
            ConversationSession existing = conversationSessionMapper.selectOne(
                    new LambdaQueryWrapper<ConversationSession>()
                            .eq(ConversationSession::getConversationId, conversationId)
                            .eq(ConversationSession::getUserId, userId)
                            .eq(ConversationSession::getDeleted, false)
                            .last("limit 1"));
            if (existing == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "会话不存在或已删除");
            }
            return existing;
        }
        return createConversation(userId, accessKeyId, titleHint);
    }

    @Override
    public List<ConversationSummaryDto> listRecent(Integer userId, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        Page<ConversationSession> page = new Page<>(1, pageSize);
        Page<ConversationSession> result = conversationSessionMapper.selectPage(page,
                new LambdaQueryWrapper<ConversationSession>()
                        .eq(ConversationSession::getUserId, userId)
                        .eq(ConversationSession::getDeleted, false)
                        .orderByDesc(ConversationSession::getPinned, ConversationSession::getLastActiveAt)
        );
        return result.getRecords().stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessageDto> listMessages(String conversationId, Integer userId, Integer limit, LocalDateTime before) {
        ConversationSession session = ensureConversation(userId, null, conversationId, null);
        if (session.getDeleted() != null && session.getDeleted()) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会话已删除");
        }
        int fetchLimit = limit == null ? 50 : Math.min(Math.max(limit, 1), 200);
        List<ConversationMessageDocument> docs;
        if (before != null) {
            docs = conversationMessageRepository.findTop50ByConversationIdAndUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(conversationId, userId, before);
        } else {
            docs = conversationMessageRepository.findTop50ByConversationIdAndUserIdOrderByCreatedAtDesc(conversationId, userId);
        }
        if (docs.size() > fetchLimit) {
            docs = docs.subList(0, fetchLimit);
        }
        Collections.reverse(docs); // 返回按时间正序，便于前端展示
        return docs.stream().map(this::toMessageDto).collect(Collectors.toList());
    }

    @Override
    public ConversationSession updateConversation(String conversationId, Integer userId, String title, Boolean pinned) {
        ConversationSession session = ensureConversation(userId, null, conversationId, null);
        boolean changed = false;
        if (StringUtils.hasText(title)) {
            session.setTitle(title);
            changed = true;
        }
        if (pinned != null) {
            session.setPinned(pinned);
            changed = true;
        }
        if (changed) {
            session.setUpdatedAt(LocalDateTime.now());
            conversationSessionMapper.updateById(session);
        }
        return session;
    }

    @Override
    public void deleteConversation(String conversationId, Integer userId) {
        ConversationSession session = ensureConversation(userId, null, conversationId, null);
        session.setDeleted(true);
        session.setUpdatedAt(LocalDateTime.now());
        conversationSessionMapper.updateById(session);
    }

    @Override
    public ConversationAppendResult appendChatMessages(String conversationId,
                                                       Integer userId,
                                                       Integer accessKeyId,
                                                       String userContent,
                                                       List<String> userImageUrls,
                                                       String assistantContent,
                                                       String modelIdentifier,
                                                       Integer promptTokens,
                                                       Integer completionTokens,
                                                       BigDecimal cost) {
        ConversationSession session = ensureConversation(userId, accessKeyId, conversationId, userContent);
        LocalDateTime now = LocalDateTime.now();
        int savedCount = 0;
        String assistantMessageId = null;

        if (StringUtils.hasText(userContent) || !CollectionUtils.isEmpty(userImageUrls)) {
            ConversationMessageDocument userDoc = buildMessageDoc(session.getConversationId(), userId, "user", userContent, userImageUrls, modelIdentifier, null, null, null, now.minusNanos(1000000));
            conversationMessageRepository.save(userDoc);
            savedCount++;
        }
        if (StringUtils.hasText(assistantContent)) {
            ConversationMessageDocument assistantDoc = buildMessageDoc(session.getConversationId(), userId, "assistant", assistantContent, null, modelIdentifier, promptTokens, completionTokens, cost, now);
            ConversationMessageDocument stored = conversationMessageRepository.save(assistantDoc);
            assistantMessageId = stored.getId();
            savedCount++;
        }

        session.setAccessKeyId(accessKeyId);
        session.setLastModelIdentifier(modelIdentifier);
        session.setLastMessageSummary(truncate(assistantContent, 200));
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + savedCount);
        session.setLastActiveAt(now);
        session.setUpdatedAt(now);
        conversationSessionMapper.updateById(session);

        return new ConversationAppendResult(session.getConversationId(), assistantMessageId);
    }

    @Override
    public ConversationTail getRecentTail(String conversationId, Integer userId, int limit) {
        List<ConversationMessageDto> messages = listMessages(conversationId, userId, limit, null);
        return new ConversationTail(conversationId, messages);
    }

    private ConversationMessageDocument buildMessageDoc(String conversationId,
                                                        Integer userId,
                                                        String role,
                                                        String content,
                                                        List<String> imageUrls,
                                                        String modelIdentifier,
                                                        Integer promptTokens,
                                                        Integer completionTokens,
                                                        BigDecimal cost,
                                                        LocalDateTime createTime) {
        ConversationMessageDocument doc = new ConversationMessageDocument();
        doc.setConversationId(conversationId);
        doc.setUserId(userId);
        doc.setRole(role);
        doc.setContent(content);
        doc.setImageUrls(imageUrls);
        doc.setModelIdentifier(modelIdentifier);
        doc.setPromptTokens(promptTokens);
        doc.setCompletionTokens(completionTokens);
        doc.setCost(cost);
        doc.setCreatedAt(createTime == null ? LocalDateTime.now() : createTime);
        return doc;
    }

    private ConversationSummaryDto toSummaryDto(ConversationSession session) {
        ConversationSummaryDto dto = new ConversationSummaryDto();
        BeanUtils.copyProperties(session, dto);
        return dto;
    }

    private ConversationMessageDto toMessageDto(ConversationMessageDocument doc) {
        ConversationMessageDto dto = new ConversationMessageDto();
        dto.setMessageId(doc.getId());
        dto.setConversationId(doc.getConversationId());
        dto.setRole(doc.getRole());
        dto.setContent(doc.getContent());
        dto.setImageUrls(doc.getImageUrls());
        dto.setModelIdentifier(doc.getModelIdentifier());
        dto.setPromptTokens(doc.getPromptTokens());
        dto.setCompletionTokens(doc.getCompletionTokens());
        dto.setCost(doc.getCost());
        dto.setCreatedAt(doc.getCreatedAt());
        return dto;
    }

    private String truncate(String content, int max) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        return content.length() <= max ? content : content.substring(0, max);
    }

    private String generateConversationId() {
        return "c_" + UUID.randomUUID().toString().replace("-", "");
    }
}
