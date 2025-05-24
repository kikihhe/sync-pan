package com.xiaohe.pan.common.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class HeartbeatDTO {
    /**
     * 设备ID
     */
    private String deviceKey;

    /**
     * 设备密钥
     */
    private String secret;

    /**
     * 该设备已经绑定的目录的远程路径
     */
    private List<String> boundDirectoryRemotePath;
}
