package com.xiaohe.pan.server.web.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadChunkFileDTO {
    /**
     * 所属文件的唯一标识, 为所属文件的 md5
     */
    private String identifier;

    /**
     * 分片名称，为该分片的md5
     */
    private String chunkName;

    /**
     * 分片所属文件的类型
     */
    private String fileType;

    /**
     * 分片序号
     */
    private Integer chunkNumber;
    /**
     * 当前分片的大小
     */
    private Long currentChunkSize;
    /**
     * 文件一共分为多少片
     */
    private Integer totalChunks;

    /**
     * 分片所属文件的总大小
     */
    private Long totalSize;

    /**
     * 分片所属文件 所在的目录
     */
    private Long menuId;

    /**
     * 用户id
     */
    private Long owner;

    /**
     * 分片
     */
    private MultipartFile multipartFile;
}
