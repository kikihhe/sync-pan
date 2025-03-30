package com.xiaohe.pan.client.service;

import com.alibaba.fastjson.JSON;
import com.xiaohe.pan.client.config.ClientConfig;
import com.xiaohe.pan.client.http.HttpClientManager;
import com.xiaohe.pan.client.listener.FileListenerMonitor;
import com.xiaohe.pan.client.model.BoundMenu;
import com.xiaohe.pan.client.model.dto.DeviceHeartbeatDTO;
import com.xiaohe.pan.client.model.vo.DeviceHeartbeatVO;
import com.xiaohe.pan.common.util.Result;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class HeartbeatService {

    private final HttpClientManager httpClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final FileListenerMonitor monitor = FileListenerMonitor.getInstance();

    public HeartbeatService(HttpClientManager httpClient) {
        this.httpClient = httpClient;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 60, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        try {
            DeviceHeartbeatDTO dto = new DeviceHeartbeatDTO();
            dto.setDeviceKey(ClientConfig.getDeviceKey());
            dto.setSecret(ClientConfig.getSecret());

            String data = JSON.toJSONString(dto);
            String response = httpClient.post("/device/heartbeat", data);

            Result result = JSON.parseObject(response, Result.class);
            if (Objects.isNull(result)) {
                throw new RuntimeException("获取的result为空, 请检查请求的发送以及服务端的响应");
            }
            if (result.getCode() != 200) {
                throw new RuntimeException(result.getMessage());
            }
            DeviceHeartbeatVO vo = (DeviceHeartbeatVO) result.getData();
            processBinding(vo.getPendingBindings());
        } catch (Exception e) {
            System.err.println("心跳发送失败: " + e.getMessage());
        }
    }

    private void processBinding(List<BoundMenu> bindings) {
        if (bindings == null || bindings.isEmpty()) return;
        
        bindings.forEach(binding -> {
            boolean success = monitor.bindDirectory(binding.getLocalPath(), binding.getRemoteMenuPath().toString());
            System.out.println("绑定目录 " + binding.getLocalPath() + " => " + (success ? "成功" : "失败"));
        });
    }

    public void shutdown() throws IOException {
        scheduler.shutdown();
        httpClient.close();
    }
}
