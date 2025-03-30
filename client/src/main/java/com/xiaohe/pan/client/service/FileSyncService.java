package com.xiaohe.pan.client.service;

import com.xiaohe.pan.client.event.EventContainer;
import com.xiaohe.pan.client.http.HttpClientManager;
import com.xiaohe.pan.client.listener.FileListenerMonitor;
import com.xiaohe.pan.client.model.Event;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileSyncService {
    private final ScheduledExecutorService scheduler;
    private final EventContainer eventContainer;
    private final HttpClientManager httpClient;

    public FileSyncService(HttpClientManager httpClient) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.eventContainer = EventContainer.getInstance();
        this.httpClient = httpClient;
    }


    public void start(FileListenerMonitor monitor) {
        scheduler.scheduleAtFixedRate(this::processEvents, 5, 3, TimeUnit.SECONDS);
        monitor.start();
    }

    private void processEvents() {
        try {
            List<Event> events = eventContainer.drainEvents();
            if (events.isEmpty()) {
                return;
            }
            List<Event> mergedEvents = EventContainer.mergeEvents(events);
            for (Event event : mergedEvents) {
                System.out.println("处理合并后的事件: " + event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}