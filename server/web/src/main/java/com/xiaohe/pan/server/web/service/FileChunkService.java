package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaohe.pan.server.web.model.domain.FileChunk;
import com.xiaohe.pan.server.web.model.dto.MergeChunkFileDTO;
import com.xiaohe.pan.server.web.model.dto.UploadChunkFileDTO;

import java.io.IOException;
import java.util.List;

public interface FileChunkService extends IService<FileChunk> {
    Boolean uploadChunkFile(UploadChunkFileDTO chunkFileDTO) throws IOException;

    List<FileChunk> getUploadedChunk(String identifier, Long userId);

    Boolean mergeChunk(MergeChunkFileDTO fileDTO) throws IOException;
}
