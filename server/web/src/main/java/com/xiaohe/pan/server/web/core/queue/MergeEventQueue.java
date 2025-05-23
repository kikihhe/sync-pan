package com.xiaohe.pan.server.web.core.queue;

import com.xiaohe.pan.common.model.dto.MergeEvent;
import com.xiaohe.pan.server.web.model.dto.ResolveConflictDTO;
import com.xiaohe.pan.server.web.model.vo.FileConflictVO;
import com.xiaohe.pan.server.web.model.vo.MenuConflictVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class MergeEventQueue {
    private BlockingQueue<MergeEvent> mergeEventQueue = new LinkedBlockingQueue<>();
    /**
     * key: device_key
     */
    private ConcurrentHashMap<String, BlockingQueue<ResolveConflictDTO>> resolveConflictQueue = new ConcurrentHashMap<>();
    public void addEvent(MergeEvent event) {
        mergeEventQueue.add(event);
    }
    public void addResolveConflict(String deviceKey, ResolveConflictDTO resolveConflictDTO) {
            resolveConflictQueue.computeIfAbsent(deviceKey, k -> new LinkedBlockingQueue<>(10))
                    .offer(resolveConflictDTO);
    }
    public MergeEvent pollEvent() {
        return mergeEventQueue.poll();
    }
    public List<ResolveConflictDTO> pollResolveConflict(String deviceKey) throws InterruptedException {
        BlockingQueue<ResolveConflictDTO> queue = resolveConflictQueue.get(deviceKey);
        if (queue == null) {
            return new ArrayList<>();
        }
        ResolveConflictDTO poll = queue.poll(3, TimeUnit.SECONDS);
        if (poll == null) {
            return null;
        }
        List<ResolveConflictDTO> resolveConflictVOS = new ArrayList<>();
        resolveConflictVOS.add(poll);
        resolveConflictQueue.clear();
        return resolveConflictVOS;
    }

    public void clearResolveConflict(String deviceKey) throws InterruptedException {
        resolveConflictQueue.remove(deviceKey);
    }
    public boolean isEmpty(String deviceKey) {
        BlockingQueue<ResolveConflictDTO> resolveConflictDTOS = resolveConflictQueue.get(deviceKey);
        return CollectionUtils.isEmpty(resolveConflictDTOS);
    }

//    public List<MergeEvent> pollAllEvents() throws InterruptedException {
//        MergeEvent poll = mergeEventQueue.poll(1, TimeUnit.MINUTES);
//        if (poll != null) {
//            List<MergeEvent> events = new ArrayList<>(mergeEventQueue);
//            events.add(poll);
//            mergeEventQueue.clear();
//            return events;
//        }
//        return null;
//    }
}
