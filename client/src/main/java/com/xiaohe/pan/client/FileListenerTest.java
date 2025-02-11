//package com.xiaohe.pan.client;
//
//import com.xiaohe.pan.client.event.EventProcessor;
//import com.xiaohe.pan.client.listener.FileListenerMonitor;
//
//import java.io.File;
//
//public class FileListenerTest {
//
//    public static void main(String[] args) {
//        try {
//            FileListenerMonitor monitor = FileListenerMonitor.getInstance();
//            monitor.bindDirectory(new File("/Users/kikihhe/Documents"), "/kikihhe/mac/Documents");
//            monitor.bindDirectory(new File("/Users/kikihhe/Download"), "/kikihhe/mac/Download");
//
//            // 启动事件处理器
//            EventProcessor.getInstance().start();
//
//            // 启动文件监听
//            monitor.start();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
