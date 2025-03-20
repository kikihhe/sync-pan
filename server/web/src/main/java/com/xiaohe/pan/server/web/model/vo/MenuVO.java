package com.xiaohe.pan.server.web.model.vo;


import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MenuVO {
    private Menu currentMenu;
    private Integer total;
    private Integer pageNum;
    private Integer pageSize;
    /**
     * 由于查看时不需要树形，所以 subMenu 的类型为 Menu
     */
    private List<MenuDetailVO> subMenuList;

    private List<File> subFileList;

}
