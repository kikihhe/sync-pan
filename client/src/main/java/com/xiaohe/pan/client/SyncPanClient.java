package com.xiaohe.pan.client;

import com.xiaohe.pan.client.config.ClientConfig;
import com.xiaohe.pan.client.service.FileSyncService;
import com.xiaohe.pan.client.http.HttpClientManager;
import com.xiaohe.pan.client.listener.FileListenerMonitor;
import com.xiaohe.pan.client.service.HeartbeatService;
import com.xiaohe.pan.client.storage.MD5StorageFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SyncPanClient {
    private static final String SERVER_BASE_URL = "http://localhost:8080";

    public static void main(String[] args) {
        // 解析命令行参数
        Map<String, String> params = parseArgs(args);
        ClientConfig.init(params.get("device-key"), params.get("secret"));

        // 初始化组件
        HttpClientManager httpClient = new HttpClientManager(SERVER_BASE_URL);


        FileListenerMonitor monitor = FileListenerMonitor.getInstance();
        HeartbeatService heartbeatService = new HeartbeatService(httpClient, MD5StorageFactory.getInstance());
        FileSyncService fileSyncService = new FileSyncService(httpClient);

        // 启动服务
        monitor.start();
        heartbeatService.start();
        fileSyncService.start();

        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                fileSyncService.shutdown();
                heartbeatService.shutdown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println("客户端已安全关闭");
        }));
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    params.put(parts[0], parts[1]);
                }
            }
        }
        return params;
    }
}
