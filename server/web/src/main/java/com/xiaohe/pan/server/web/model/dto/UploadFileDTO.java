package com.xiaohe.pan.server.web.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class UploadFileDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long menuId;
    private Long owner;
    private MultipartFile multipartFile;
//    private List<Long> fileIdList;
//    private List<Long> menuIdList;
}
