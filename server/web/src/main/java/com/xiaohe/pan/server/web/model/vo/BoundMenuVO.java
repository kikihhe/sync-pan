package com.xiaohe.pan.server.web.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoundMenuVO {
    private Long id;
    private String deviceName;
    private String localPath;
    private String remoteMenuPath;
    private Long remoteMenuId;
    private Integer direction;
    private LocalDateTime lastSyncedAt;
}