package com.xiaohe.pan.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.pan.client.event.EventContainer;
import com.xiaohe.pan.client.http.HttpClientManager;
import com.xiaohe.pan.client.listener.FileListenerMonitor;
import com.xiaohe.pan.client.model.BoundDirectory;
import com.xiaohe.pan.client.model.Event;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileSyncService {
    private final ScheduledExecutorService scheduler;
    private final EventContainer eventContainer;
    private final HttpClientManager httpClient;
    private final FileListenerMonitor monitor = FileListenerMonitor.getInstance();

    public FileSyncService(HttpClientManager httpClient) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.eventContainer = EventContainer.getInstance();
        this.httpClient = httpClient;
    }


    public void start() {
        scheduler.scheduleAtFixedRate(this::processEvents, 5, 300, TimeUnit.SECONDS);
    }

    private void processEvents() {
        try {
            List<Event> events = eventContainer.drainEvents();
            if (events.isEmpty()) {
                return;
            }
            List<Event> mergedEvents = EventContainer.mergeEvents(events);
            for (Event event : mergedEvents) {
                processEvent(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processEvent(Event event) throws IOException {
        BoundDirectory boundDirectory = monitor.getBoundDirectoryByRemotePath(event.getRemoteMenuPath());
        event.setRemoteMenuId(boundDirectory.getRemoteMenuId());
        Map<String, Object> data = new HashMap<>();
        data.put("localPath", event.getRelativePath());
        data.put("remoteMenuId", event.getRemoteMenuId());
        data.put("remoteMenuPath", event.getRemoteMenuPath());
        data.put("type", event.getType());
        String jsonData = new ObjectMapper().writeValueAsString(data);
        String resp = httpClient.upload("/bound/sync", event.getFile(), jsonData);
        System.out.println(resp);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}