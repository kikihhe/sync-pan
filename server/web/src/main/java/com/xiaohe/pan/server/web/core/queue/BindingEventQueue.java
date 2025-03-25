package com.xiaohe.pan.server.web.core.queue;

import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import org.springframework.stereotype.Service;

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
        BlockingQueue<BoundMenu> queue = eventQueueMap.get(deviceKey);
        if (queue != null) {
            List<BoundMenu> events = new ArrayList<>();
            queue.drainTo(events);
            return events;
        } else {
            try {
                BoundMenu poll = queue.poll(1, TimeUnit.MINUTES);
                List<BoundMenu> events = new ArrayList<>();
                queue.drainTo(events);
                events.add(poll);
                return events;
            } catch (InterruptedException e) {
                throw new BusinessException("handle过程中出错，message: " + e.getMessage());
            }
        }
    }
}