package com.xiaohe.pan.server.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaohe.pan.server.web.model.domain.Secret;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecretMapper extends BaseMapper<Secret> {
}
