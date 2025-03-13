package com.xiaohe.pan.client;

import com.xiaohe.pan.client.event.EventProcessor;
import com.xiaohe.pan.client.listener.FileListenerMonitor;

public class FileListenerTest {

    public static void main(String[] args) {
        try {
            FileListenerMonitor monitor = FileListenerMonitor.getInstance();
            monitor.bindDirectory("/Users/kikihhe/Documents", "/kikihhe/mac/Documents");
            monitor.bindDirectory("/Users/kikihhe/Download", "/kikihhe/mac/Download");

            // 启动事件处理器
            EventProcessor.getInstance().start(monitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
