package com.xiaohe.pan.server.web.core.queue;

import com.xiaohe.pan.common.model.dto.MergeEvent;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MergeEventQueue {
    private ConcurrentLinkedQueue<MergeEvent> mergeEventQueue = new ConcurrentLinkedQueue<>();


    public void addEvent(MergeEvent event) {
        mergeEventQueue.add(event);
    }

    public MergeEvent pollEvent() {
        return mergeEventQueue.poll();
    }
    public boolean isEmpty() {
        return mergeEventQueue.isEmpty();
    }
    public List<MergeEvent> pollAllEvents() {
        List<MergeEvent> events = new ArrayList<>(mergeEventQueue);
        mergeEventQueue.clear();
        return events;
    }
}
