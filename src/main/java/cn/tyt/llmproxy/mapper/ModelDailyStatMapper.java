package cn.tyt.llmproxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.tyt.llmproxy.entity.ModelDailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ModelDailyStatMapper extends BaseMapper<ModelDailyStat> {

    /**
     * 插入或更新模型的每日统计数据（原子操作）
     * 如果当天该模型的记录已存在，则累加计数；否则，插入新记录。
     * @param stat 要更新的统计对象
     */
    void upsert(@Param("stat") ModelDailyStat stat);
}
