package com.xiaohe.pan.common.model.dto;

import com.xiaohe.pan.common.enums.EventType;
import lombok.Data;

/**
 * 事件数据传输对象，用于客户端和服务端之间传输事件信息
 */
@Data
public class EventDTO {
    /**
     * 文件相对路径
     */
    private String localPath;
    
    /**
     * 远程菜单ID
     */
    private Long remoteMenuId;
    
    /**
     * 远程菜单路径
     */
    private String remoteMenuPath;
    
    /**
     * 事件类型
     */
    private EventType type;
    
    /**
     * 事件发生的时间戳（毫秒）
     */
    private long timestamp;
    
    /**
     * 设备ID
     */
    private Long deviceId;
    
    /**
     * 设备密钥
     */
    private String secret;
}