package cn.tyt.llmproxy.repository;

import cn.tyt.llmproxy.document.UsageLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for accessing UsageLog data in MongoDB.
 */
@Repository
// MongoRepository<EntityType, IdType>
public interface UsageLogRepository extends MongoRepository<UsageLogDocument, String> {

    // Spring Data MongoDB will automatically implement this method based on its name!
    // Finds all logs for a specific user.
    List<UsageLogDocument> findByModelId(Integer modelId);
    List<UsageLogDocument> findByUserId(Integer userId);
    List<UsageLogDocument> findByAccessKeyId(Integer accessKeyId);
    // You can also create more complex queries using method names.
    List<UsageLogDocument> findByUserIdAndIsSuccess(Integer userId, Boolean isSuccess);
}