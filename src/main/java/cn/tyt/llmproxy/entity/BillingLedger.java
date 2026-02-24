package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("billing_ledger")
public class BillingLedger implements Serializable {

    public static final String STATUS_INIT = "INIT";
    public static final String STATUS_SETTLED = "SETTLED";

    public static final String BIZ_CHAT = "CHAT";
    public static final String BIZ_IMAGE = "IMAGE";
    public static final String BIZ_TOPUP = "TOPUP";

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private Integer userId;
    private Integer accessKeyId;
    private Integer modelId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer imageCount;
    private BigDecimal amount;
    private String status;
    private String bizType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
