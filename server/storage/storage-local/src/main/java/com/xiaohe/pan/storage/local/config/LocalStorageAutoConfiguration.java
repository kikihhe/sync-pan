package com.xiaohe.pan.storage.local.config;

import com.xiaohe.pan.storage.api.StorageService;
import com.xiaohe.pan.storage.local.LocalStorageService;
import com.xiaohe.pan.storage.local.property.LocalStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LocalStorageProperties.class)
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "local")
public class LocalStorageAutoConfiguration {
    
    @Bean
    public StorageService localStorageService(LocalStorageProperties properties) {
        return new LocalStorageService(properties);
    }
}