package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    // 状态常量
    public static final Integer STATUS_ACTIVE = 1;
    public static final Integer STATUS_INACTIVE = 0;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String passwordHash;
    private String email;
    private String role;
    private BigDecimal balance;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Version
    private Integer version; // 用于乐观锁
}