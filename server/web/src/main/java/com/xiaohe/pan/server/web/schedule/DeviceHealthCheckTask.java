package com.xiaohe.pan.server.web.schedule;


import com.xiaohe.pan.server.web.mapper.DeviceMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class DeviceHealthCheckTask {

    private final Logger logger = LoggerFactory.getLogger(DeviceHealthCheckTask.class);

    @Resource
    private DeviceMapper deviceMapper;

    @Scheduled(fixedRate = 5 * 60 * 1000) // 5分钟检查一次
    public void checkDeviceStatus() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        // 标记死亡设备
        int affected = deviceMapper.markDeadDevices(threshold);
        if (affected > 0) {
            logger.warn("标记 {} 个设备为死亡状态", affected);
        }
    }
}
