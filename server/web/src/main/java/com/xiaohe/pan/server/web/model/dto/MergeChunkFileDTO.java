package com.xiaohe.pan.server.web.model.dto;

import lombok.Data;

import java.io.Serializable;


@Data
public class MergeChunkFileDTO implements Serializable {

    private String fileName;

    private String identifier;

    private Long totalSize;

    private Long menuId;
}
