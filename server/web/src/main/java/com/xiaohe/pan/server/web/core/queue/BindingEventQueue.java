package com.xiaohe.pan.server.web.core.queue;

import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class BindingEventQueue {

    /**
     * 设备Key与待处理事件的映射
     */
    private final ConcurrentMap<String, BlockingQueue<BoundMenu>> eventQueueMap = new ConcurrentHashMap<>();

    /**
     * 添加绑定事件到队列
     * @param deviceKey
     * @param boundMenu
     */
    public void addEvent(String deviceKey, BoundMenu boundMenu) {
        eventQueueMap.computeIfAbsent(deviceKey, k -> new LinkedBlockingQueue<>(10))
                .offer(boundMenu);
    }

    /**
     * 获取设备待处理事件
     * @param deviceKey
     * @return
     */
    public List<BoundMenu> pollEvents(String deviceKey) throws BusinessException {
        BlockingQueue<BoundMenu> queue = eventQueueMap.computeIfAbsent(deviceKey, k -> {
            return new LinkedBlockingQueue<>(10);
        });
        List<BoundMenu> events = new ArrayList<>();
        try {
            BoundMenu firstEvent = queue.poll(1, TimeUnit.MINUTES);
            if (firstEvent != null) {
                events.add(firstEvent);
                // 取出剩余所有元素
                queue.drainTo(events);
            }
            return events;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("操作被中断: " + e.getMessage());
        }
    }
}