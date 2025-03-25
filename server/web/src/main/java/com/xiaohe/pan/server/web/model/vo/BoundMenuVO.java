package com.xiaohe.pan.server.web.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoundMenuVO {
    private Long id;
    private String deviceName;
    private String localPath;
    private String remotePath;
    private Integer direction;
    private LocalDateTime lastSyncedAt;
}