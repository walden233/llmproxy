package cn.tyt.llmproxy.mapper;

import cn.tyt.llmproxy.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE users SET balance = balance + #{delta}, version = version + 1, updated_at = NOW() " +
            "WHERE id = #{userId} AND balance + #{delta} >= 0")
    int updateBalance(@Param("userId") Integer userId, @Param("delta") java.math.BigDecimal delta);
}
