package cn.tyt.llmproxy.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class StatisticsQueryDto {

    //要查询的模型ID（数据库主键），可选
    private Integer modelId;

    //查询开始日期 (格式: yyyy-MM-dd)，可选。如果只提供此参数，则查询单日数据。
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    //查询结束日期 (格式: yyyy-MM-dd)，可选。如果提供，则与startDate构成日期范围。
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}