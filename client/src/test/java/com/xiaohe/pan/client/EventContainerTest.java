package com.xiaohe.pan.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.pan.client.service.FileSyncService;
import com.xiaohe.pan.client.listener.FileListenerMonitor;
import lombok.Data;
import org.junit.Test;


public class EventContainerTest {
    public ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Data
    class User {
        private String name;
        private int age;
    }
    @Test
    public void testEventProcessing() throws JsonProcessingException {
        FileListenerMonitor monitor = FileListenerMonitor.getInstance();

//        monitor.bindDirectory("C:\\Users\\23825\\Desktop\\毕业设计\\毕设客户端Test1", "/kikihhe/windows/desktop/毕设客户端Test1");
//        monitor.bindDirectory("C:\\Users\\23825\\Desktop\\毕业设计\\毕设客户端Test2", "/kikihhe/windows/desktop/毕设客户端Test1");
        User user = new User();
        user.setName("kiki");
        user.setAge(20);
        String s = OBJECT_MAPPER.writeValueAsString(user);
        System.out.println(s);

    }
}
