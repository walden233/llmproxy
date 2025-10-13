package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("provider_keys")
public class ProviderKey {
    // 状态常量
    public static final Integer STATUS_ACTIVE = 1;
    public static final Integer STATUS_INACTIVE = 0;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer providerId;
    private String apiKey;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}