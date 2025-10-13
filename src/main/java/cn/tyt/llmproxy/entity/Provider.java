package cn.tyt.llmproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("providers")
public class Provider {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String urlBase;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}