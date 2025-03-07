package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaohe.pan.server.web.model.domain.File;

import java.util.List;

public interface FileService extends IService<File> {

    public List<File> getSubFileByRange(Long menuId, Long userId, Integer start, Integer count);

    public Long countByMenuId(Long menuId, Long userId);
}
