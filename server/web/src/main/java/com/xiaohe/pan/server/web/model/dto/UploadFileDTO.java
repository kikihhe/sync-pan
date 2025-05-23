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
}
