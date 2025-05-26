package com.xiaohe.pan.client.service;

import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.TypeReference;
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
import com.xiaohe.pan.common.util.PathUtils;
import com.xiaohe.pan.common.util.Result;
import org.apache.commons.collections4.CollectionUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        scheduler.scheduleAtFixedRate(this::processEvents, 5, 30, TimeUnit.SECONDS);
        downScheduler.scheduleAtFixedRate(this::mergedEvents, 5, 30, TimeUnit.SECONDS);
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
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " 进行同步");
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
        try {
            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " 拉取合并事件");
            List<MergeEvent> events = fetchMergeEventsFromServer();
            if (CollectionUtils.isEmpty(events)) {
                System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " 没有需要处理的合并事件");
                return;
            }
            // 把 localBoundMenuPath 中的 \\ 替换为 /
            events.forEach(mergeEvent -> {
                mergeEvent.setLocalBoundMenuPath(normalizePathSeparator(mergeEvent.getLocalBoundMenuPath()));
                mergeEvent.setLocalPath(calculateLocalPath(mergeEvent.getLocalBoundMenuPath(), mergeEvent.getRemoteBoundMenuPath(), mergeEvent.getRemoteMenuPath(), mergeEvent.getFilename()));
            });
            // 按本地绑定目录分组
            Map<String, List<MergeEvent>> mergeEventMap = events.stream().collect(Collectors.groupingBy(MergeEvent::getLocalBoundMenuPath));
            // 处理每个分组
            mergeEventMap.forEach((localBoundMenuPath, eventList) -> {
                // 根据路径找出当前分组的公共前缀
                List<String> localPathList = eventList.stream().map(MergeEvent::getLocalPath).collect(Collectors.toList());
                String commonPrefix = PathUtils.findCommonPrefix(localPathList);
                // 清除目录
                File targetDir = new File(commonPrefix);
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                if (targetDir.exists()) {
                    deleteRecursiveWithoutSelf(targetDir);
                }
                eventList.stream().sorted(Comparator.comparing(MergeEvent::getFileType)).forEach(event -> {
                    String fullPath = event.getLocalPath();
                    if (event.getFileType() == 1) { // 目录
                        new File(fullPath).mkdirs();
                    } else { // 文件
                        try {
                            Path path = Paths.get(fullPath);
                            Path parentDir = path.getParent();
                            if (Files.notExists(parentDir)) {
                                Files.createDirectories(parentDir);
                            }
                            // 覆盖写入
                            Files.write(path, event.getData());
                        } catch (IOException e) {
                            System.out.println("文件创建失败：" + fullPath);
                        }
                    }
                });
            });
//            Map<String, List<MergeEvent>> dirGroups = events.stream()
//                    .collect(Collectors.groupingBy(e -> {
//                        // 计算本地绑定根目录：localBoundMenuPath + (remoteMenuPath - remoteBoundMenuPath)
//                        return calculateLocalMenu(
//                                e.getLocalBoundMenuPath(),
//                                e.getRemoteBoundMenuPath(),
//                                e.getRemoteMenuPath()
//                        );
//                    }));
//            // 按路径长度排序（路径越短越先处理）
//            List<Map.Entry<String, List<MergeEvent>>> sortedGroups = dirGroups.entrySet().stream()
//                    .sorted(Comparator.comparingInt(entry -> entry.getKey().length()))
//                    .collect(Collectors.toList());
//
//            dirGroups.forEach((localDir, eventList) -> {
//                // 暴力清除目录
//                File targetDir = new File(localDir);
//                if (targetDir.exists()) {
//                    deleteRecursiveWithoutSelf(targetDir);
//                }
//                targetDir.mkdirs();
//
//                // 批量创建新内容
//                eventList.stream().sorted().forEach(event -> {
//                    String fullPath = calculateLocalPath(
//                            event.getLocalBoundMenuPath(),
//                            event.getRemoteBoundMenuPath(),
//                            event.getRemoteMenuPath(),
//                            event.getFilename()
//                    );
//                    if (event.getFileType() == 1) { // 目录
//                        new File(fullPath).mkdirs();
//                    } else { // 文件
//                        try {
//                            Path path = Paths.get(fullPath);
//                            Path parentDir = path.getParent();
//                            if (Files.notExists(parentDir)) {
//                                Files.createDirectories(parentDir);
//                            }
//                            // 覆盖写入
//                            Files.write(path, event.getData());
//                        } catch (IOException e) {
//                            System.out.println("文件创建失败：" + fullPath);
//                        }
//                    }
//                });
//            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // 从服务端获取合并事件
    private List<MergeEvent> fetchMergeEventsFromServer() throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("deviceKey", ClientConfig.getDeviceKey());
        map.put("secret", ClientConfig.getSecret());
        String response = httpClient.post("/bound/getMergedEvents", map, "");
        Result<List<MergeEvent>> result = JSON.parseObject(
                response,
                new TypeReference<Result<List<MergeEvent>>>(){}
        );
        if (result == null) {
            System.out.println("获取合并事件失败,result is null");
            return new ArrayList<>();
        }
        if (result.getCode() != 200) {
            System.out.println("获取合并事件失败, result: " + result);
            return new ArrayList<>();
        }
        return result.getData();
    }
//    private void processEventsByType(List<MergeEvent> events, int fileType) {
//
//    }
//    // 根据路径深度排序，越浅的路径越先处理
//    private int compareByPathDepth(MergeEvent e1, MergeEvent e2) {
//        return Integer.compare(
//                e1.getLocalBoundMenuPath().split("/").length,
//                e2.getLocalBoundMenuPath().split("/").length
//        );
//    }
//    private void handleSingleEvent(MergeEvent mergeEvent) {
//        String targetPath = calculateLocalPath(
//                mergeEvent.getLocalBoundMenuPath(),
//                mergeEvent.getRemoteBoundMenuPath(),
//                mergeEvent.getRemoteMenuPath(),
//                mergeEvent.getFilename()
//        );
//        eventContainer.addMergedEvent(targetPath);
//        File targetFile = new File(targetPath);
//        try {
//            // 1. 创建
//            if (mergeEvent.getType() == 1) {
//                // 1.1 目录
//                if (mergeEvent.getFileType() == 1) {
//                    targetFile.mkdirs();
//                } else {
//                    // 1.2 文件
//                    if (targetFile.exists()) {
//                        targetFile.delete();
//                    }
//                    ByteInputStream byteInputStream = new ByteInputStream(mergeEvent.getData(), mergeEvent.getData().length);
//                    try {
//                        FileUtils.writeStream2File(byteInputStream, targetFile, (long) mergeEvent.getData().length);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            // 2. 删除
//            else if (mergeEvent.getType() == 2) {
//                // 不管文件还是目录直接删
//                if (targetFile.exists()) {
//                    Files.delete(targetFile.toPath());
//                }
//            }
//            // 3. 修改
//            else if (mergeEvent.getType() == 3) {
//                // 只提供修改名称功能
//                if (!mergeEvent.getOldFileName().equals(mergeEvent.getFilename()) && targetFile.exists()) {
//                    targetFile = new File(mergeEvent.getOldFileName());
//                    File renameTo = new File(mergeEvent.getFilename());
//                    targetFile.renameTo(renameTo);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public String calculateLocalMenu(String localBoundMenuPath,
                                            String remoteBoundMenuPath,
                                            String remoteMenuPath) {
        // 标准化路径格式
        String normalizedLocal = normalizePath(localBoundMenuPath);
        String normalizedRemoteBound = normalizePath(remoteBoundMenuPath);
        String normalizedRemote = normalizePath(remoteMenuPath);

        // 提取相对路径部分
        String relativePath = normalizedRemote.startsWith(normalizedRemoteBound) ?
                normalizedRemote.substring(normalizedRemoteBound.length()) : "";

        // 拼接完整路径
        return joinPaths(normalizedLocal, relativePath);
    }
    private static String normalizePath(String path) {
        // 统一替换反斜杠为正斜杠，并去除首尾斜杠
        return path.replace('\\', '/')
                .replaceAll("^/+|/+$", "");
    }
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
        String a = filename.isEmpty() ? "" : "/" + filename;
        if (relativePath.isEmpty()) {
            fullLocalPath = normalizedLocalBoundPath + a;
        } else {
            fullLocalPath = normalizedLocalBoundPath + "/" + relativePath + a;
        }
        return fullLocalPath;
    }
    private String joinPaths(String base, String addition) {
        // 智能拼接路径部分
        if (base.isEmpty()) return addition;
        if (addition.isEmpty()) return base;
        return base + "/" + addition;
    }
    /**
     * 标准化路径分隔符，将所有斜杠转换为统一格式
     */
    private String normalizePathSeparator(String path) {
        // 将所有反斜杠替换为正斜杠，然后再处理
        return path.replace('\\', '/');
    }
    // 递归删除整个目录（含自身）
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteRecursive(entry); // 递归删除子项
                }
            }
        }
        file.delete();
    }

    // 仅删除子内容（保留当前目录）
    private void deleteRecursiveWithoutSelf(File dir) {
        if (dir.isDirectory()) {
            File[] entries = dir.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteRecursive(entry);
                }
            }
        }
    }

    public void shutdown() throws Exception {
        monitor.stop();
        scheduler.shutdown();
        System.out.println("fileSyncService shutdown");
    }
}