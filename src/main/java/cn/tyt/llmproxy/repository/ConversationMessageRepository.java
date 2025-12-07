package cn.tyt.llmproxy.repository;

import cn.tyt.llmproxy.document.ConversationMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends MongoRepository<ConversationMessageDocument, String> {

    List<ConversationMessageDocument> findByConversationIdAndUserIdOrderByCreatedAtDesc(String conversationId, Integer userId);

    List<ConversationMessageDocument> findTop50ByConversationIdAndUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            String conversationId, Integer userId, java.time.LocalDateTime before);

    List<ConversationMessageDocument> findTop50ByConversationIdAndUserIdOrderByCreatedAtDesc(
            String conversationId, Integer userId);
}
