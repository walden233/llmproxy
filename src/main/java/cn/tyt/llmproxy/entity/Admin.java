package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("admins")
public class Admin {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String passwordHash;
    private String email;
    private Integer status; // 0: 不可用, 1: 可用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}