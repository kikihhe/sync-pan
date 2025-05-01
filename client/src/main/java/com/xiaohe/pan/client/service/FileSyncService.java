package com.xiaohe.pan.client.service;

import com.alibaba.fastjson.JSON;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.xiaohe.pan.client.config.ClientConfig;
import com.xiaohe.pan.client.event.EventContainer;
import com.xiaohe.pan.client.http.HttpClientManager;
import com.xiaohe.pan.client.listener.FileListenerMonitor;
import com.xiaohe.pan.client.model.BoundDirectory;
import com.xiaohe.pan.client.model.Event;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;
import com.xiaohe.pan.common.model.dto.MergeEvent;
import com.xiaohe.pan.common.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FileSyncService {
    private final ScheduledExecutorService scheduler;
    private final ScheduledExecutorService downScheduler;
    private final EventContainer eventContainer;
    private final HttpClientManager httpClient;
    private final FileListenerMonitor monitor = FileListenerMonitor.getInstance();

    public FileSyncService(HttpClientManager httpClient) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.downScheduler = Executors.newScheduledThreadPool(1);
        this.eventContainer = EventContainer.getInstance();
        this.httpClient = httpClient;
    }


    public void start() {
        scheduler.scheduleAtFixedRate(this::processEvents, 5, 60, TimeUnit.SECONDS);
        downScheduler.scheduleAtFixedRate(this::mergedEvents, 5, 60, TimeUnit.SECONDS);
        System.out.println("fileSyncService started");
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
        eventContainer.cleanMergedEvents();
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

    private void mergedEvents() {
        // 从服务端获取需要处理的合并事件
        List<MergeEvent> mergeEventList = fetchMergeEventsFromServer();
        // 把 localBoundMenuPath 中的 \\ 替换为 /
        mergeEventList.forEach(mergeEvent -> mergeEvent.setLocalBoundMenuPath(normalizePathSeparator(mergeEvent.getLocalBoundMenuPath())));
        // 处理目录事件（按路径深度排序）
        processEventsByType(mergeEventList, 1);
        // 处理文件事件（按路径深度排序）
        processEventsByType(mergeEventList, 2);
    }

    // 从服务端获取合并事件
    private List<MergeEvent> fetchMergeEventsFromServer() {
        // 暂时返回空列表
        return new ArrayList<>();
    }
    private void processEventsByType(List<MergeEvent> events, int fileType) {
        events.stream()
                .filter(e -> e.getFileType() == fileType)
                .sorted(this::compareByPathDepth)
                .forEach(this::handleSingleEvent);
    }
    // 根据路径深度排序，越浅的路径越先处理
    private int compareByPathDepth(MergeEvent e1, MergeEvent e2) {
        return Integer.compare(
                e1.getLocalBoundMenuPath().split("/").length,
                e2.getLocalBoundMenuPath().split("/").length
        );
    }
    private void handleSingleEvent(MergeEvent mergeEvent) {
        String targetPath = calculateLocalPath(
                mergeEvent.getLocalBoundMenuPath(),
                mergeEvent.getRemoteBoundMenuPath(),
                mergeEvent.getRemoteMenuPath(),
                mergeEvent.getFilename()
        );
        eventContainer.addMergedEvent(targetPath);
        File targetFile = new File(targetPath);
        try {
            if (mergeEvent.getType() == 1) { // 创建
                // 目录
                if (mergeEvent.getFileType() == 1) {
                    targetFile.mkdirs();
                } else {
                    // 文件
                    if (targetFile.exists()) {
                        targetFile.delete();
                    }
                    ByteInputStream byteInputStream = new ByteInputStream(mergeEvent.getData(), mergeEvent.getData().length);
                    try {
                        FileUtils.writeStream2File(byteInputStream, targetFile, (long) mergeEvent.getData().length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mergeEvent.getType() == 2) { // 删除
                // 不管文件还是目录直接删
                if (targetFile.exists()) {
                    Files.delete(targetFile.toPath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    private void mergedEvents() {
//        // 假设已经通过请求拿到了所有合并事件
//        List<MergeEvent> mergeEventList = new ArrayList<>();
//        // 先处理目录再处理文件
//        List<MergeEvent> menuMergeEventList = mergeEventList.stream().filter(mergeEvent -> mergeEvent.getFileType() == 1).sorted((e1, e2) -> {
//            String[] path1 = e1.getLocalBoundMenuPath().split("/");
//            String[] path2 = e2.getLocalBoundMenuPath().split("/");
//            return path1.length - path2.length;
//        }).collect(Collectors.toList());
//        List<MergeEvent> fileMergeEventList = mergeEventList.stream().filter(mergeEvent -> mergeEvent.getFileType() == 2).sorted((e1, e2) -> {
//            String[] path1 = e1.getLocalBoundMenuPath().split("/");
//            String[] path2 = e2.getLocalBoundMenuPath().split("/");
//            return path1.length - path2.length;
//        }).collect(Collectors.toList());
//        // 遍历每个合并事件
//        // 1. 目录
//        for (MergeEvent mergeEvent : menuMergeEventList) {
//            // 处理每个合并事件
//            // 这里可以根据需要进行进一步的处理，比如更新数据库、发送通知等
//            // 1. 要处理的是哪个文件
//            // 本地的绑定目录
//            String localBoundPath = mergeEvent.getLocalBoundMenuPath();
//            // 云端的绑定目录
//            String remoteBoundPath = mergeEvent.getRemoteMenuPath();
//            // 文件/目录名
//            String name = mergeEvent.getFilename();
//            // 获取要处理的本地目录名称
//            String targetFileName = calculateLocalPath(localBoundPath, remoteBoundPath, mergeEvent.getRemoteMenuPath(), name);
//            // 事件类型，增/删
//            Integer type = mergeEvent.getType();
//
//            File file = new File(targetFileName);
//
//            if (type == 1) {
//                // 增加事件，创建目录
//                file.delete();
//            } else {
//                // 删除事件，删除目录
//                file.delete();
//            }
//        }
//
//        for (MergeEvent mergeEvent : fileMergeEventList) {
//            String localBoundPath = mergeEvent.getLocalBoundMenuPath();
//            String remoteBoundPath = mergeEvent.getRemoteMenuPath();
//            String name = mergeEvent.getFilename();
//            String targetFileName = calculateLocalPath(localBoundPath, remoteBoundPath, mergeEvent.getRemoteMenuPath(), name);
//            Integer type = mergeEvent.getType();
//            File file = new File(targetFileName);
//            if (!file.isFile()) continue;
//            if (type == 1) {
//                // 文件增加事件
//                if (file.exists()) {
//                    file.delete();
//                }
//                ByteInputStream byteInputStream = new ByteInputStream(mergeEvent.getData(), mergeEvent.getData().length);
//                try {
//                    FileUtils.writeStream2File(byteInputStream, file, (long) mergeEvent.getData().length);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                // 文件删除事件
//                if (file.exists()) {
//                    file.delete();
//                }
//            }
//        }
//
//    }

    private String calculateLocalPath(String localBoundMenuPath, String remoteBoundMenuPath, String remoteMenuPath, String filename) {
        // 标准化路径分隔符，确保在不同操作系统下都能正确处理
        localBoundMenuPath = normalizePathSeparator(localBoundMenuPath);
        remoteBoundMenuPath = normalizePathSeparator(remoteBoundMenuPath);
        remoteMenuPath = normalizePathSeparator(remoteMenuPath);

        // 确保本地绑定顶级目录不以斜杠结尾
        String normalizedLocalBoundPath = localBoundMenuPath.endsWith("/")
                ? localBoundMenuPath.substring(0, localBoundMenuPath.length() - 1)
                : localBoundMenuPath;

        // 确保远端绑定目录不以斜杠结尾
        String normalizedRemoteBoundPath = remoteBoundMenuPath.endsWith("/")
                ? remoteBoundMenuPath.substring(0, remoteBoundMenuPath.length() - 1)
                : remoteBoundMenuPath;

        // 提取云端的相对路径部分（绑定目录之后的部分）
        String relativePath = "";
        if (remoteMenuPath.startsWith(normalizedRemoteBoundPath)) {
            // +1 是为了跳过路径分隔符
            int startIndex = normalizedRemoteBoundPath.length() + 1;
            if (startIndex <= remoteMenuPath.length()) {
                relativePath = remoteMenuPath.substring(startIndex);
            }
        }

        // 构建完整本地路径
        String fullLocalPath;
        if (relativePath.isEmpty()) {
            fullLocalPath = normalizedLocalBoundPath + "/" + filename;
        } else {
            fullLocalPath = normalizedLocalBoundPath + "/" + relativePath + "/" + filename;
        }

        return fullLocalPath;
    }

    /**
     * 标准化路径分隔符，将所有斜杠转换为统一格式
     */
    private String normalizePathSeparator(String path) {
        // 将所有反斜杠替换为正斜杠，然后再处理
        return path.replace('\\', '/');
    }

    public void shutdown() throws Exception {
        monitor.stop();
        scheduler.shutdown();
        System.out.println("fileSyncService shutdown");
    }
}