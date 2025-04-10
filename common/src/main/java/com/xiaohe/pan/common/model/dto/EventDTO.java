package com.xiaohe.pan.common.model.dto;

import com.xiaohe.pan.common.enums.EventType;
import lombok.Data;

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

    /**
     * 文件关联索引，用于关联上传的文件
     * 如果是目录，则为null
     */
    private Integer fileIndex;
}