package com.xiaohe.pan.server.web.controller;

import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.constants.FileConstants;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.FileChunk;
import com.xiaohe.pan.server.web.model.dto.DeleteFileDTO;
import com.xiaohe.pan.server.web.model.dto.MergeChunkFileDTO;
import com.xiaohe.pan.server.web.model.dto.UpdateFileDTO;
import com.xiaohe.pan.server.web.model.dto.UploadChunkFileDTO;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.model.vo.FileChunkVO;
import com.xiaohe.pan.server.web.service.FileChunkService;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.util.HttpUtil;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    @PostMapping(value = "/upload")
    public Result<String> uploadFile(UploadFileDTO fileDTO) throws IOException {
        if (Objects.isNull(fileDTO)) {
            return Result.error("请选择文件并填写参数");
        }
        Boolean nameDuplicate = fileService.checkNameDuplicate(fileDTO.getMenuId(), fileDTO.getFileName());
        if (nameDuplicate) {
            return Result.error("文件名重复!");
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

    @PostMapping("/updateFile")
    public Result<String> updateFileName(@RequestBody UpdateFileDTO file) throws RuntimeException {
        if (Objects.isNull(file.getId())) {
            return Result.error("修改失败");
        }
        File byId = fileService.getById(file.getId());
        if (!Objects.equals(byId.getOwner(), file.getOwner())) {
            return Result.error("权限不足");
        }
        File rawFile = new File();
        BeanUtils.copyProperties(file, rawFile);
        fileService.updateById(rawFile);
        return Result.success("修改成功");
    }

    @GetMapping("/download")
    public void download(@RequestParam("id") Long id, HttpServletResponse response) throws RuntimeException, IOException {
        if (Objects.isNull(id)) {
            throw new BusinessException("请选择文件");
        }
        fileService.download(id, response);
    }

    /**
     * 文件预览
     * @return
     */
    @GetMapping("/preview")
    public void previewFile(@RequestParam("fileId") Long fileId, HttpServletRequest request, HttpServletResponse response) throws RuntimeException, IOException {
        if (Objects.isNull(fileId)) {
            throw new BusinessException("请选择文件");
        }
        addCommonResponseHeader(response, request.getContentType());
        fileService.preview(fileId, response);

    }
    /**
     * 添加公共的文件读取响应头
     */
    private void addCommonResponseHeader(HttpServletResponse response, String contentTypeValue) {
        response.reset();
        HttpUtil.addCorsResponseHeaders(response);
        response.addHeader(FileConstants.CONTENT_TYPE_STR, contentTypeValue);
        response.setContentType(contentTypeValue);
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
