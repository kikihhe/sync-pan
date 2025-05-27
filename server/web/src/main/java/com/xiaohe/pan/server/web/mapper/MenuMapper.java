package com.xiaohe.pan.server.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaohe.pan.server.web.model.domain.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    public List<Menu> selectSubMenuByRange(@Param("menuId") Long menuId,
                                           @Param("userId") Long userId,
                                           @Param("name") String name,
                                           @Param("orderBy") Integer orderBy,
                                           @Param("desc") Integer desc,
                                           @Param("start") Integer start,
                                           @Param("count") Integer count);

    public List<Menu> selectAllMenusByMenuId(@Param("menuId") Long menuId);

    int resolve(@Param("menu") Menu menu);
}
