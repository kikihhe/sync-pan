package com.xiaohe.pan.client.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xiaohe.pan.client.config.ClientConfig;
import com.xiaohe.pan.client.http.HttpClientManager;
import com.xiaohe.pan.client.listener.FileListenerMonitor;
import com.xiaohe.pan.client.model.BoundMenu;
import com.xiaohe.pan.client.model.BoundMenuEvent;
import com.xiaohe.pan.client.model.vo.DeviceHeartbeatVO;
import com.xiaohe.pan.client.storage.MD5StorageFactory;
import com.xiaohe.pan.common.util.Result;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class HeartbeatService {

    private final HttpClientManager httpClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final FileListenerMonitor monitor = FileListenerMonitor.getInstance();
    private final MD5StorageFactory md5StorageFactory;

    public HeartbeatService(HttpClientManager httpClient, MD5StorageFactory md5StorageFactory) {
        this.httpClient = httpClient;
        this.md5StorageFactory = md5StorageFactory;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 2, TimeUnit.MINUTES);
        System.out.println("heartbeatService started");
    }

    private void sendHeartbeat() {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("deviceKey", ClientConfig.getDeviceKey());
            map.put("secret", ClientConfig.getSecret());

            System.out.println("发送心跳" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            String response = httpClient.post("/device/heartbeat", map, "");

            Result<DeviceHeartbeatVO> result = JSON.parseObject(
                    response,
                    new TypeReference<Result<DeviceHeartbeatVO>>(){}
            );
            if (Objects.isNull(result)) {
                throw new RuntimeException("获取的result为空, 请检查请求的发送以及服务端的响应");
            }
            if (result.getCode() != 200) {
                System.out.println(result);
                throw new RuntimeException(result.getMessage());
            }
            // 服务端返回 505 表示设备已被删除
            if (result.getCode() == 50001 || result.getCode() == 50002) {
                System.out.println("设备不存在，程序停止");
                System.exit(1);
            }
            DeviceHeartbeatVO vo = result.getData();
            System.out.println("心跳成功!");
            if (vo.getSyncCommand() == 1) {
                processEvents(vo.getPendingBindings());
            }
        } catch (Exception e) {
            System.err.println("心跳发送失败: " + e.getMessage());
        }
    }

    public void processEvents(List<BoundMenuEvent> bindings) {
        if (bindings == null || bindings.isEmpty()) return;
        
        bindings.forEach(event -> {
            BoundMenu binding = event.getBoundMenu();
            if (event.getType() == 1) {
                boolean success = monitor.bindDirectory(binding.getLocalPath(), binding.getRemoteMenuPath().toString(), binding.getRemoteMenuId());
                System.out.println("绑定目录 " + binding.getLocalPath() + " => " + binding.getRemoteMenuPath() + " " + (success ? "成功" : "失败"));
                if (success) {
                    // 绑定成功后，创建或获取 MD5 存储器
                    try {
                        md5StorageFactory.createOrGetStorage(binding.getLocalPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("MD5Storage 初始化: " + binding.getLocalPath());
                } else {
                    // 如果绑定监听器失败，不应该创建 MD5 storage
                    // 或者尝试移除可能存在的旧 storage
                    Path localPath = Paths.get(binding.getLocalPath());
                    md5StorageFactory.removeStorage(localPath);
                }
            } else if (event.getType() == 2) {
                boolean success = monitor.unbindDirectory(binding.getLocalPath(), binding.getRemoteMenuId());
                if (success) {
                    md5StorageFactory.removeStorage(Paths.get(binding.getLocalPath()));
                }
                System.out.println("解绑目录 " + binding.getLocalPath() + " " + (success? "成功" : "失败"));
            }

        });
    }

    public void shutdown() throws IOException {
        System.out.println("heartbeatService shutdown");
        scheduler.shutdown();
        httpClient.close();
    }
}
