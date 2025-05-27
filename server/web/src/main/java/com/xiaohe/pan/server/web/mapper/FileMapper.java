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

    // 分页查询回收站内的文件
    List<File> selectDeletedFiles(
            @Param("userId") Long userId,
            @Param("fileName") String fileName,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    List<File> selectAllFilesByMenuId(
            @Param("menuId") Long menuId
    );


    // 回收站内文件的计数
    Long countDeletedFiles(@Param("userId") Long userId, @Param("fileName") String fileName);

    // 持久化删除
    int permanentDeleteById(@Param("fileId") Long fileId);

    File getDeletedFileById(@Param("fileId") Long fileId);

    int updateForRecycle(@Param("fileId") Long fileId,
                         @Param("targetMenuId") Long targetMenuId);

    /**
     * 查询30天前被删除的文件
     * @return 文件列表
     */
    List<File> selectFilesDeletedBefore30Days();

    int recycle(@Param("file") File file);
}
