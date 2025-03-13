package com.xiaohe.pan.server.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFileDTO {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private Integer storageType;

    private Long menuId;

    private Long owner;

    private MultipartFile multipartFile;
}
