package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.server.web.convert.PageConvert;
import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    /**
     * 获取指定目录下的文件
     */
    @Override
    public List<File> getSubFileByRange(Long menuId, Long userId, Integer start, Integer count) {
        return baseMapper.selectSubFileByRange(menuId, userId, start, count);
    }

    @Override
    public Long countByMenuId(Long menuId, Long userId) {
        LambdaQueryWrapper<File> lambda = new LambdaQueryWrapper<>();
        lambda.eq(File::getMenuId, menuId);
        lambda.eq(File::getOwner, userId);
        return count(lambda);
    }

    @Override
    public Boolean uploadFile(MultipartFile multipartFile, UploadFileDTO fileDTO) {
        return null;
    }
}
