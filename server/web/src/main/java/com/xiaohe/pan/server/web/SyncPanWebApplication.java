package com.xiaohe.pan.server.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ServletComponentScan(basePackages = {"com.xiaohe.pan.server.web.filter"})
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
public class SyncPanWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncPanWebApplication.class, args);
    }

}
