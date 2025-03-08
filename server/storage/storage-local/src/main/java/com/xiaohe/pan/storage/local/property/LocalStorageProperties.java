package com.xiaohe.pan.storage.local.property;



import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "storage.local")
@Component
public class LocalStorageProperties {
    /**
     * 存放所有文件的目录
     */
    @Value("${storage.local.root-path}")
    private String rootFilePath;

    /**
     * 实际存放所有文件分片的路径
     */
    @Value("${storage.local.root-chunk-path}")
    private String rootFileChunkPath;
}
