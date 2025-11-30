package cn.tyt.llmproxy.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class StatisticsQueryDto {

    //要查询的模型ID（数据库主键），可选，与modelIdentifier二选一，优先此字段
    private Integer modelId;
    //要查询的模型名，可选
    private String modelIdentifier;

    //单日查询日期 (格式: yyyy-MM-dd)，可选。若设置了 startDate/endDate，则忽略此字段
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    //起始日期 (格式: yyyy-MM-dd)，可选
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    //结束日期 (格式: yyyy-MM-dd)，可选
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
