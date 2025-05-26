package com.xiaohe.pan.server.web.model.vo;

import com.xiaohe.pan.server.web.model.domain.Menu;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class MenuConflictVO {
    private Menu menu;
    private String oldName;
    /**
     * 1: 新增
     * 2: 删除
     * 3: 修改
     */
    private Integer type;
    private ArrayList<FileConflictVO> subFileList;
    private ArrayList<MenuConflictVO> submenuList;
}
