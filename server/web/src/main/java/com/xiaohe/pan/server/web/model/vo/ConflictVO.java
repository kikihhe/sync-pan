package com.xiaohe.pan.server.web.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConflictVO {
    private ArrayList<FileConflictVO> fileConflictVOList;
    private ArrayList<MenuConflictVO> menuConflictVOList;
}
