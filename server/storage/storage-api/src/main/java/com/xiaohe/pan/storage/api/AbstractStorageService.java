package com.xiaohe.pan.storage.api;


import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import com.xiaohe.pan.storage.api.context.MergeFileContext;
import com.xiaohe.pan.storage.api.context.ReadFileContext;
import com.xiaohe.pan.storage.api.context.StoreFileChunkContext;
import com.xiaohe.pan.storage.api.context.StoreFileContext;

import java.io.IOException;

public abstract class AbstractStorageService implements StorageService {
    @Override
    public void delete(DeleteFileContext context) throws IOException {

    }

    @Override
    public void mergeFile(MergeFileContext context) throws IOException {

    }

    @Override
    public void realFile(ReadFileContext context) throws IOException {

    }

    @Override
    public void store(StoreFileContext context) throws IOException {

    }

    @Override
    public void storeChunk(StoreFileChunkContext context) throws IOException {

    }
}
