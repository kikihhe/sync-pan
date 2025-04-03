package com.xiaohe.pan.server.web.model.dto;

import com.xiaohe.pan.common.enums.EventType;
import lombok.Data;

@Data
public class BoundMenuDTO {
    private Long deviceId;
    private Integer direction;
    private String localPath;
    private Long remoteMenuId;
    private String remoteMenuPath;
    private Integer status;
    private EventType type;
}
