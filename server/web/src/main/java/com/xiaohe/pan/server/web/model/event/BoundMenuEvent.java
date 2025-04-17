package com.xiaohe.pan.server.web.model.event;

import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import lombok.Data;

@Data
public class BoundMenuEvent {

    private BoundMenu boundMenu;
    /**
     * 事件类型
     * 1. 绑定
     * 2. 解绑
     */
    private Integer type;
}
