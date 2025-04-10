package com.xiaohe.pan.server.web.model.dto;

import com.xiaohe.pan.common.util.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeletedFileQueryDTO extends PageQuery {
    
    /**
     * 模糊搜索文件名（非必填）
     */
    private String fileName;

    // 可扩展其他查询条件
    public DeletedFileQueryDTO() {
    }

    // 带分页参数的构造函数
    public DeletedFileQueryDTO(Integer pageNum, Integer pageSize) {
        super(pageNum, pageSize);
    }
}
