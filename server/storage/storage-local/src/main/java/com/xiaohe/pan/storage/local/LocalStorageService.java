package com.xiaohe.pan.storage.local;


import com.xiaohe.pan.storage.api.AbstractStorageService;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import com.xiaohe.pan.storage.local.property.LocalStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;

@ConditionalOnProperty(prefix = "storage", value = "local")
public class LocalStorageService extends AbstractStorageService {
    private LocalStorageProperties properties;

    public LocalStorageService(LocalStorageProperties properties) {
        this.properties = properties;
    }


    @Override
    public void store(StoreFileContext context) throws IOException {
        System.out.println(properties.getRootFilePath());
        System.out.println(properties.getRootFileChunkPath());
    }
}
