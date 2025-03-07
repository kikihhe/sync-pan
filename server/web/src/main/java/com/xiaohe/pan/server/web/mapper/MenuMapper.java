package com.xiaohe.pan.server.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaohe.pan.server.web.model.domain.Menu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    public List<Menu> selectSubMenuByRange(Long menuId, Long userId, Integer start, Integer count);
}
