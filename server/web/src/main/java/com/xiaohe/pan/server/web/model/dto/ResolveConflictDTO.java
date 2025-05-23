package com.xiaohe.pan.server.web.model.dto;

import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import lombok.Data;

import java.util.List;

@Data
public class ResolveConflictDTO {
    /**
     * 当前解决冲突的目录ID
     */
    private Long currentMenuId;

    private Menu currentMenu;

    private List<Menu> menuItems;

    private List<File> fileItems;
}


