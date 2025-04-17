package com.xiaohe.pan.common.model.vo;

import com.xiaohe.pan.common.enums.EventType;
import lombok.Data;

@Data
public class EventVO {
    private String localPath;
    private String remotePath;
    private EventType eventType;
}
