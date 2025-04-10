package com.xiaohe.pan.common.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量事件数据传输对象，用于客户端向服务端批量传输多个事件
 */
@Data
public class EventsDTO {
    /**
     * 事件列表
     */
    private List<EventDTO> events;
    
    /**
     * 设备ID
     */
    private Long deviceId;
    
    /**
     * 设备密钥
     */
    private String secret;
}