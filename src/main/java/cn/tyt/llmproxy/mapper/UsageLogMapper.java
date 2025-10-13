package cn.tyt.llmproxy.mapper;

import cn.tyt.llmproxy.entity.UsageLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UsageLogMapper extends BaseMapper<UsageLog> {
}