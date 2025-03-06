package com.xiaohe.pan.server.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan(basePackages = {"com.xiaohe.pan.server.web.filter"})
public class SyncPanWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncPanWebApplication.class, args);
    }

}
