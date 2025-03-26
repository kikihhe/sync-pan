package com.xiaohe.pan.client.model.vo;


import com.xiaohe.pan.client.model.BoundMenu;
import lombok.Data;

import java.util.List;


@Data
public class DeviceHeartbeatVO {
    /**
     * 绑定目录
     */
    private List<BoundMenu> pendingBindings;

    /**
     * 当前心跳是否携带绑定目录
     */
    private Integer syncCommand;
}
