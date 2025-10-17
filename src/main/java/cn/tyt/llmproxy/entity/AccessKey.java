package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("access_keys")
public class AccessKey implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String keyValue;
    private Integer userId;
    private Boolean isActive; // 使用 Boolean 映射 TINYINT(1)
    private LocalDateTime createdAt;
}