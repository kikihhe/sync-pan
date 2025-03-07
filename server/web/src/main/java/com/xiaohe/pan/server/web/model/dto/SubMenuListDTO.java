package com.xiaohe.pan.server.web.model.dto;

import com.xiaohe.pan.common.util.PageQuery;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 用于获取指定目录的子目录
 */
@NoArgsConstructor
@AllArgsConstructor
public class SubMenuListDTO extends PageQuery {
    private Long menuId;

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
