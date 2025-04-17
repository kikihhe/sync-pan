package com.xiaohe.pan.server.web.model.vo;

import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.event.BoundMenuEvent;
import lombok.Data;

import java.util.List;


@Data
public class DeviceHeartbeatVO {
    /**
     * 绑定目录
     */
    private List<BoundMenuEvent> pendingBindings;

    /**
     * 当前心跳是否携带绑定目录
     */
    private Integer syncCommand;
}
