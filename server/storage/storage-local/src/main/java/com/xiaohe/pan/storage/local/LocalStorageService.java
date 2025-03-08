package com.xiaohe.pan.storage.local;


import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.FileUtils;
import com.xiaohe.pan.storage.api.AbstractStorageService;
import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import com.xiaohe.pan.storage.api.context.MergeFileContext;
import com.xiaohe.pan.storage.api.context.ReadFileContext;
import com.xiaohe.pan.storage.api.context.StoreFileChunkContext;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import com.xiaohe.pan.storage.local.property.LocalStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.CollectionUtils;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(prefix = "storage", value = "local")
public class LocalStorageService extends AbstractStorageService {
    private LocalStorageProperties properties;

    public LocalStorageService(LocalStorageProperties properties) {
        this.properties = properties;
    }
    /**
     * 如果使用 local 方式，启动时要检查 root path 是否存在，不存在就创建
     * @throws Exception
     */
    @PostConstruct
    public void initPath() throws Exception {
        String rootFilePath = properties.getRootFilePath();
        String rootFileChunkPath = properties.getRootFileChunkPath();
        File rootDir = new File(rootFilePath);
        File chunkDir = new File(rootFileChunkPath);
        if (!rootDir.exists()) {
            org.apache.commons.io.FileUtils.forceMkdir(rootDir);
        }
        if (!chunkDir.exists()) {
            org.apache.commons.io.FileUtils.forceMkdir(chunkDir);
        }
    }

    @Override
    public void doStore(StoreFileContext context) throws IOException {
        // 生成文件的真正路径
        String basePath = properties.getRootFilePath();
        String realFilePath = FileUtils.generateStoreFileRealPath(basePath, context.getFilename());
        // 存储文件
        InputStream inputStream = context.getInputStream();
        File realFile = new File(realFilePath);
        Long totalSize = context.getTotalSize();
        FileUtils.writeStream2File(inputStream, realFile, totalSize);
        context.setRealPath(realFilePath);
    }

    @Override
    public void doDelete(DeleteFileContext context) throws RuntimeException {
        List<String> realFilePathList = context.getRealFilePathList();
        if (CollectionUtils.isEmpty(realFilePathList)) {
            return;
        }
        realFilePathList.forEach(filePath -> {
            try {
                org.apache.commons.io.FileUtils.forceDelete(new File(filePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Override
    public void doRealFile(ReadFileContext context) throws IOException {
        File file = new File(context.getRealPath());
        FileInputStream inputStream = new FileInputStream(file);
        FileUtils.writeFile2OutputStream(inputStream, context.getOutputStream(), file.length());
    }


    @Override
    public void doStoreChunk(StoreFileChunkContext context) throws IOException {

    }

    @Override
    public void doMergeFile(MergeFileContext context) throws IOException {

    }
}
