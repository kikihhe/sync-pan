package com.xiaohe.pan.server.web.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class DeleteFileDTO {
    private List<Long> fileList;
}
