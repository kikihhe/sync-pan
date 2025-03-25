package com.xiaohe.pan.server.web.model.dto;

import lombok.Data;

@Data
public class DeviceHeartbeatDTO {

    /**
     * 设备唯一标识
     */
    private String deviceKey;

    /**
     * 同步密钥
     */
    private String secret;
}
