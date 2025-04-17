package com.xiaohe.pan.server.web.model.vo;

import com.xiaohe.pan.server.web.model.domain.Menu;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MenuConflictVO {
    private Menu menu;
    /**
     * 1: 新增
     * 2: 删除
     */
    private Integer type;
}
