package com.xiaohe.pan.client.service;

import com.alibaba.fastjson.JSON;

import com.xiaohe.pan.client.config.ClientConfig;
import com.xiaohe.pan.client.event.EventContainer;
import com.xiaohe.pan.client.http.HttpClientManager;
import com.xiaohe.pan.client.listener.FileListenerMonitor;
import com.xiaohe.pan.client.model.BoundDirectory;
import com.xiaohe.pan.client.model.Event;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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
        scheduler.scheduleAtFixedRate(this::processEvents, 5, 100, TimeUnit.SECONDS);
    }

    private void processEvents() {
        try {
            List<Event> events = eventContainer.drainEvents();
            if (events.isEmpty()) {
                return;
            }
            List<Event> mergedEvents = EventContainer.mergeEvents(events);
            if (!mergedEvents.isEmpty()) {
                processBatchEvents(mergedEvents);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processBatchEvents(List<Event> events) throws IOException {
        if (events.isEmpty()) {
            return;
        }

        // 创建EventsDTO对象
        EventsDTO eventsDTO = new EventsDTO();
        List<EventDTO> eventDTOList = new ArrayList<>();

        // 设置设备ID和密钥
        eventsDTO.setDeviceKey(ClientConfig.getDeviceKey());
        eventsDTO.setSecret(ClientConfig.getSecret());

        // 转换所有事件为EventDTO
        for (Event event : events) {
            BoundDirectory boundDirectory = monitor.getBoundDirectoryByRemotePath(event.getRemoteMenuPath());
            if (boundDirectory != null) {
                event.setRemoteMenuId(boundDirectory.getRemoteMenuId());

                EventDTO eventDTO = new EventDTO();
                eventDTO.setLocalPath(event.getRelativePath());
                eventDTO.setRemoteMenuId(event.getRemoteMenuId());
                eventDTO.setRemoteMenuPath(event.getRemoteMenuPath());
                eventDTO.setType(event.getType());
                eventDTO.setTimestamp(event.getTimestamp());

                // 读出文件内容
                if (event.getFile() != null && event.getFile().exists() && !event.getFile().isDirectory()) {
                    String localPath = event.getRelativePath();
                    byte[] bytes = Files.readAllBytes(new File(localPath).toPath());
                    eventDTO.setData(bytes);
                }

                eventDTOList.add(eventDTO);
            }
        }

        eventsDTO.setEvents(eventDTOList);

        // 序列化EventsDTO为JSON
        String jsonData = JSON.toJSONString(eventsDTO);

        // 发送请求，使用普通的POST请求，因为文件已经包含在JSON中
        String resp = httpClient.post("/bound/sync", jsonData);
        System.out.println("批量同步响应: " + resp);
    }

//    private void processEvent(Event event) throws IOException {
//        BoundDirectory boundDirectory = monitor.getBoundDirectoryByRemotePath(event.getRemoteMenuPath());
//        event.setRemoteMenuId(boundDirectory.getRemoteMenuId());
//        Map<String, Object> data = new HashMap<>();
//        data.put("localPath", event.getRelativePath());
//        data.put("remoteMenuId", event.getRemoteMenuId());
//        data.put("remoteMenuPath", event.getRemoteMenuPath());
//        data.put("type", event.getType());
//        String jsonData = new ObjectMapper().writeValueAsString(data);
//        String resp = httpClient.upload("/bound/sync", event.getFile(), jsonData);
//        System.out.println(resp);
//    }

    public void shutdown() throws Exception {
        monitor.stop();
        scheduler.shutdown();
    }
}