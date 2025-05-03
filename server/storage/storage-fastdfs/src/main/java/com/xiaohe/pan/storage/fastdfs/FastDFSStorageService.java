package com.xiaohe.pan.storage.fastdfs;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.xiaohe.pan.common.constants.SyncPanConstants;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.FileUtils;
import com.xiaohe.pan.storage.api.AbstractStorageService;
import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import com.xiaohe.pan.storage.api.context.MergeFileContext;
import com.xiaohe.pan.storage.api.context.ReadFileContext;
import com.xiaohe.pan.storage.api.context.StoreFileChunkContext;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import com.xiaohe.pan.storage.fastdfs.config.FastDFSStorageEngineConfig;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@ConditionalOnProperty(prefix = "storage", value = "fastdfs")
public class FastDFSStorageService extends AbstractStorageService {

    @Resource
    private FastFileStorageClient client;

    @Resource
    private FastDFSStorageEngineConfig config;


    @Override
    public void doDelete(DeleteFileContext context) throws IOException {
        List<String> realFilePathList = context.getRealFilePathList();
        if (CollectionUtils.isNotEmpty(realFilePathList)) {
            realFilePathList.stream().forEach(client::deleteFile);
        }
    }

    @Override
    public void doMergeFile(MergeFileContext context) throws IOException {
        throw new BusinessException("FastDFS不支持分片上传的操作");
    }

    @Override
    public void doRealFile(ReadFileContext context) throws IOException {
        String realPath = context.getRealPath();
        String group = realPath.substring(SyncPanConstants.ZERO_INT, realPath.indexOf(SyncPanConstants.SLASH_STR));
        String path = realPath.substring(realPath.indexOf(SyncPanConstants.SLASH_STR) + SyncPanConstants.ONE_INT);

        DownloadByteArray downloadByteArray = new DownloadByteArray();
        byte[] bytes = client.downloadFile(group, path, downloadByteArray);

        OutputStream outputStream = context.getOutputStream();
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }

    @Override
    public void doStore(StoreFileContext context) throws IOException {
        StorePath storePath = client.uploadFile(config.getGroup(),
                context.getInputStream(),
                context.getTotalSize(),
                FileUtils.getFileExtName(context.getFilename()));
        context.setRealPath(storePath.getFullPath());
    }

    @Override
    public void doStoreChunk(StoreFileChunkContext context) throws IOException {
        throw new BusinessException("FastDFS不支持分片上传的操作");
    }
}
