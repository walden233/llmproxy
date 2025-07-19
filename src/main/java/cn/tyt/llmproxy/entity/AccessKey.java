package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("access_keys")
public class AccessKey {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String keyValue;

    private Integer adminId;

    private Integer isActive; // 1: 可用, 0: 不可用

    private LocalDateTime createdAt;
}