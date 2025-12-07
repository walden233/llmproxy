package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("conversation_sessions")
public class ConversationSession implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;
    private Integer userId;
    private Integer accessKeyId;
    private String title;
    private Boolean pinned;
    private String lastModelIdentifier;
    private String lastMessageSummary;
    private Integer messageCount;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
}
