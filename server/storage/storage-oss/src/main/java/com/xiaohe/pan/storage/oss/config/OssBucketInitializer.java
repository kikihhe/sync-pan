package com.xiaohe.pan.storage.oss.config;

import com.aliyun.oss.OSSClient;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.storage.oss.property.OssStorageEngineProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OssBucketInitializer implements CommandLineRunner {

    @Autowired
    private OssStorageEngineProperty config;

    @Autowired
    private OSSClient client;

    @Override
    public void run(String... args) throws Exception {
        boolean bucketExist = client.doesBucketExist(config.getBucketName());

        if (!bucketExist && config.getAutoCreateBucket()) {
            client.createBucket(config.getBucketName());
            log.info("bucket not exist!, create it now");
        }

        if (!bucketExist && !config.getAutoCreateBucket()) {
            throw new BusinessException("the bucket " + config.getBucketName() + " is not available");
        }
    }

}