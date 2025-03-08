package com.xiaohe.pan.server.web.controller;

import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;
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
}
