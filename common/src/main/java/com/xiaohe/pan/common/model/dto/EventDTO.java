package com.xiaohe.pan.common.model.dto;

import com.xiaohe.pan.common.enums.EventType;
import lombok.Data;

import java.io.File;

/**
 * 事件数据传输对象，用于客户端和服务端之间传输事件信息
 */
@Data
public class EventDTO {
    /**
     * 文件绝对路径
     */
    private String localPath;
    
    /**
     * 绑定的远程菜单ID（顶级菜单）
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
     * 文件内容
     * 如果是目录，则为null
     */
    private byte[] data;
}