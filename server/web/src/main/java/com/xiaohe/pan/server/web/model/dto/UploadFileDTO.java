package com.xiaohe.pan.server.web.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class UploadFileDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Long menuId;
    private String identifier;
    private Integer source;
    private Long boundMenuId;
    private MultipartFile multipartFile;
    // 可以用multipartFile上传，也可以用字节数组上传
    private byte[] data;
}
