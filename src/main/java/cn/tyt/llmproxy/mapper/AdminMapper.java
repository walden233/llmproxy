package cn.tyt.llmproxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.tyt.llmproxy.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}