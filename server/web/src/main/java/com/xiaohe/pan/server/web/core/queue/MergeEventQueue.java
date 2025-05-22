package com.xiaohe.pan.server.web.core.queue;

import com.xiaohe.pan.common.model.dto.MergeEvent;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MergeEventQueue {
    private BlockingQueue<MergeEvent> mergeEventQueue = new LinkedBlockingQueue<>();


    public void addEvent(MergeEvent event) {
        mergeEventQueue.add(event);
    }

    public MergeEvent pollEvent() {
        return mergeEventQueue.poll();
    }

    public boolean isEmpty() {
        return mergeEventQueue.isEmpty();
    }

    public List<MergeEvent> pollAllEvents() throws InterruptedException {
        MergeEvent poll = mergeEventQueue.poll(1, TimeUnit.MINUTES);
        if (poll != null) {
            List<MergeEvent> events = new ArrayList<>(mergeEventQueue);
            events.add(poll);
            mergeEventQueue.clear();
            return events;
        }
        return null;
    }
}
