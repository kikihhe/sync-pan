package com.xiaohe.pan.client.event;

import com.xiaohe.pan.client.model.Event;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventProcessor {
    private final ScheduledExecutorService scheduler;
    private final EventContainer eventContainer;
    private static final EventProcessor INSTANCE = new EventProcessor();

    private EventProcessor() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.eventContainer = EventContainer.getInstance();
    }

    public static EventProcessor getInstance() {
        return INSTANCE;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::processEvents, 5, 3, TimeUnit.SECONDS);
    }

    private void processEvents() {
        try {
            List<Event> events = eventContainer.drainEvents();
            if (!events.isEmpty()) {
                List<Event> mergedEvents = EventContainer.mergeEvents(events);
                for (Event event : mergedEvents) {
                    System.out.println("处理合并后的事件: " + event);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}