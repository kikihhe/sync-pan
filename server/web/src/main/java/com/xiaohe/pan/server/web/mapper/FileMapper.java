package com.xiaohe.pan.server.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper extends BaseMapper<File> {
    public List<File> selectSubFileByRange(@Param("menuId") Long menuId,
                                           @Param("userId") Long userId,
                                           @Param("name") String name,
                                           @Param("orderBy") Integer orderBy,
                                           @Param("desc") Integer desc,
                                           @Param("start") Integer start,
                                           @Param("count") Integer count);

}
