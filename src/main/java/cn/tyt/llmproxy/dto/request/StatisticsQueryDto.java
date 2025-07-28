package cn.tyt.llmproxy.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class StatisticsQueryDto {

    //要查询的模型ID（数据库主键），可选
    private Integer modelId;

    private String modelIdentifier;

    //查询日期 (格式: yyyy-MM-dd)，可选。
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
}