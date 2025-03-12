package com.xiaohe.pan.server.web.model.dto;

import com.xiaohe.pan.common.util.PageQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用于获取指定目录的子目录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubMenuListDTO extends PageQuery {

    private Long menuId;

    /**
     * 文件/文件夹的名称
     */
    private String name;

    /**
     * 0: 全部
     * 1: 只看文件夹
     * 2: 只看文件
     */
    private Integer type;

    /**
     * 倒序/正序
     * 1: 倒序
     * 2: 正序
     */
    private Integer desc;

    /**
     * 1: 创建时间
     * 2: 修改时间
     */
    private Integer orderBy;

    // 手动添加包含父类字段的构造函数
    public SubMenuListDTO(Integer pageNum, Integer pageSize, Long menuId) {
        super(pageNum, pageSize); // 显式调用父类构造函数
        this.menuId = menuId;
    }

}
