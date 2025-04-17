package com.xiaohe.pan.server.web.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ConflictVO {
    private List<FileConflictVO> fileConflictVOList;
    private List<MenuConflictVO> menuConflictVOList;
}
