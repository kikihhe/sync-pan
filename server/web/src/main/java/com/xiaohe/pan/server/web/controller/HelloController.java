package com.xiaohe.pan.server.web.controller;


import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.storage.api.StorageService;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import com.xiaohe.pan.storage.local.property.LocalStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @Autowired
    private LocalStorageProperties properties;

    @Autowired
    private StorageService storageService;

    @GetMapping
    public Result<String> hello() throws IOException {
        System.out.println(properties);
        storageService.store(new StoreFileContext());
        return Result.success("你好");
    }

}
