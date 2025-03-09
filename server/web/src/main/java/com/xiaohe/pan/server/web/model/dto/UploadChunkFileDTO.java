package com.xiaohe.pan.server.web.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadChunkFileDTO {
    /**
     * 所属文件的唯一标识
     */
    private String identifier;

    private String fileName;

    private String fileType;

    private Integer totalChunks;

    private Integer chunkNumber;

    private Long currentChunkSize;

    private Long totalSize;

    private Long menuId;

    private Long owner;

    private MultipartFile multipartFile;
}
