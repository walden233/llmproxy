package cn.tyt.llmproxy.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a usage log entry stored in the 'usage_logs' collection in MongoDB.
 */
@Data
@Document(collection = "usage_logs") // Maps this class to the "usage_logs" collection
public class UsageLogDocument {

    @Id // Marks this field as the primary key (_id)
    private String id; // Use String for MongoDB's ObjectId

    @Indexed
    private Integer userId;

    @Indexed
    private Integer accessKeyId;

    @Indexed
    private Integer modelId;

    private Boolean isAsync;
    private Boolean isSuccess;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer imageCount;

    // Store cost as a string or a specific BSON decimal type for precision
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal cost;

    private LocalDateTime createTime;
}