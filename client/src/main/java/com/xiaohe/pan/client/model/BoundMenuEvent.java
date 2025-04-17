package com.xiaohe.pan.client.model;

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