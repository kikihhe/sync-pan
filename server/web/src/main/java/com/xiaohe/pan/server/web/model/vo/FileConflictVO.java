package com.xiaohe.pan.server.web.model.vo;

import com.xiaohe.pan.server.web.model.domain.File;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileConflictVO {
    private File file;
    /**
     * 1: 新增
     * 2: 删除
     * 3: 修改
     */
    private Integer type;
    private String oldName;
}
