package com.xiaohe.pan.server.web.controller;

import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.FileChunk;
import com.xiaohe.pan.server.web.model.dto.DeleteFileDTO;
import com.xiaohe.pan.server.web.model.dto.MergeChunkFileDTO;
import com.xiaohe.pan.server.web.model.dto.UploadChunkFileDTO;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.model.vo.FileChunkVO;
import com.xiaohe.pan.server.web.service.FileChunkService;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileChunkService fileChunkService;

    /**
     * 上传文件
     * @return
     */
    @PostMapping("/upload")
    public Result<String> updateFile(UploadFileDTO fileDTO) throws IOException {
        if (Objects.isNull(fileDTO)) {
            return Result.error("请选择文件并填写参数");
        }
        fileService.uploadFile(fileDTO.getMultipartFile(), fileDTO);
        return Result.success("上传成功");
    }

    @PostMapping("/delete")
    public Result<String> deleteFile(@RequestBody DeleteFileDTO fileDTO) throws IOException {
        if (CollectionUtils.isEmpty(fileDTO.getFileList())) {
            return Result.success("请选择文件");
        }
        fileService.deleteFile(fileDTO.getFileList());
        return Result.success("删除成功");
    }

    @PostMapping("/uploadChunk")
    public Result<Boolean> uploadChunk(UploadChunkFileDTO chunkFileDTO) throws IOException {
        boolean merge = fileChunkService.uploadChunkFile(chunkFileDTO);
        return Result.success("上传成功", merge);
    }

    /**
     * 查询指定标识符的上传完成的分片列表
     * @param identifier
     * @return
     */
    @GetMapping("/queryChunk")
    public Result<List<FileChunk>> queryChunk(String identifier) {
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        List<FileChunk> list = fileChunkService.getUploadedChunk(identifier, userId);
        return Result.success(list);
    }

    @PostMapping("/mergeChunk")
    public Result<String> mergeChunk(MergeChunkFileDTO fileDTO) throws IOException {
        fileChunkService.mergeChunk(fileDTO);
        return Result.success("success");
    }
}
