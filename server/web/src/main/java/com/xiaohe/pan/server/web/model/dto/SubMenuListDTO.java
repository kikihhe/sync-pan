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
    // 手动添加包含父类字段的构造函数
    public SubMenuListDTO(Integer pageNum, Integer pageSize, Long menuId) {
        super(pageNum, pageSize); // 显式调用父类构造函数
        this.menuId = menuId;
    }
    private Long menuId;
}
