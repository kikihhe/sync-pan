package com.xiaohe.pan.server.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaohe.pan.server.web.model.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
