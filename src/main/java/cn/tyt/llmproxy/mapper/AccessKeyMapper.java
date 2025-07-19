package cn.tyt.llmproxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.tyt.llmproxy.entity.AccessKey;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccessKeyMapper extends BaseMapper<AccessKey> {
}