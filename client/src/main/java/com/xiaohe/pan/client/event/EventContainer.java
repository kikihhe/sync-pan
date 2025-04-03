package com.xiaohe.pan.client.event;

import com.xiaohe.pan.common.enums.EventType;
import com.xiaohe.pan.client.model.Event;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.List;

public class EventContainer {

    private final ConcurrentLinkedQueue<Event> eventQueue;

    private final ReentrantLock lock;

    private static final EventContainer INSTANCE = new EventContainer();

    private EventContainer() {
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.lock = new ReentrantLock();
    }

    public static EventContainer getInstance() {
        return INSTANCE;
    }

    public void addEvent(Event event) {
        eventQueue.offer(event);
    }

    // 拿出所有event
    public List<Event> drainEvents() {
        List<Event> events = new ArrayList<>();
        lock.lock();
        try {
            Event event;
            while ((event = eventQueue.poll()) != null) {
                events.add(event);
            }
        } finally {
            lock.unlock();
        }
        return events;
    }


    public static List<Event> mergeEvents(List<Event> events) {
        if (events.isEmpty()) {
            return events;
        }

        // 按文件路径分组
        Map<File, List<Event>> eventsByFile = new HashMap<>();
        for (Event event : events) {
            eventsByFile.computeIfAbsent(event.getFile(), k -> new ArrayList<>()).add(event);
        }

        List<Event> mergedEvents = new ArrayList<>();

        // 对每个文件的事件进行合并
        for (List<Event> fileEvents : eventsByFile.values()) {
            // 按时间戳排序
            Collections.sort(fileEvents);

            // 获取最后一个事件，用于确定最终状态
            Event lastEvent = fileEvents.get(fileEvents.size() - 1);

            // 合并规则处理
            if (canMergeEvents(fileEvents)) {
                Event mergedEvent = getMergedEvent(fileEvents);
                if (mergedEvent != null) {
                    mergedEvents.add(mergedEvent);
                }
            } else {
                // 如果不能合并，保留最后一个事件
                mergedEvents.add(lastEvent);
            }
        }

        return mergedEvents;
    }

    private static boolean canMergeEvents(List<Event> events) {
        if (events.size() <= 1) {
            return false;
        }

        Event firstEvent = events.get(0);
        Event lastEvent = events.get(events.size() - 1);

        // 创建后删除，可以完全取消
        if (firstEvent.getType() == EventType.FILE_CREATE &&
                lastEvent.getType() == EventType.FILE_DELETE) {
            return true;
        }

        // 多次修改可以合并
        boolean allModifications = events.stream()
                .allMatch(e -> e.getType() == EventType.FILE_MODIFY);
        if (allModifications) {
            return true;
        }

        return false;
    }

    private static Event getMergedEvent(List<Event> events) {
        if (events.isEmpty()) {
            return null;
        }

        Event firstEvent = events.get(0);
        Event lastEvent = events.get(events.size() - 1);

        // 创建后删除，返回null表示不需要任何操作
        if (firstEvent.getType() == EventType.FILE_CREATE &&
                lastEvent.getType() == EventType.FILE_DELETE) {
            return null;
        }

        // 多次修改或创建后修改，返回最后一个事件
        return lastEvent;
    }
}