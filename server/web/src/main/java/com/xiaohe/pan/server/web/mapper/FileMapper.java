package com.xiaohe.pan.server.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FileMapper extends BaseMapper<File> {
    public List<File> selectSubFileByRange(Long menuId, Long userId, Integer start, Integer count);

}
