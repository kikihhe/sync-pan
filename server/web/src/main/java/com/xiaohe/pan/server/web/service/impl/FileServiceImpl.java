package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.server.web.convert.FileConvert;
import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.storage.api.StorageService;
import com.xiaohe.pan.storage.api.StoreTypeEnum;
import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.session.StoreType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Resource
    private StorageService storageService;

    @Value("${storage.type}")
    private String storageType;

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
    public Boolean uploadFile(MultipartFile multipartFile, UploadFileDTO fileDTO) throws IOException {
        File file = new File();
        try {
            // 1. 保存真实文件
            StoreFileContext storeFileContext = new StoreFileContext()
                    .setFilename(fileDTO.getFileName())
                    .setTotalSize(multipartFile.getSize())
                    .setInputStream(multipartFile.getInputStream());
            // 调用文件服务，文件的真实路径会保存在 context 中
            storageService.store(storeFileContext);

            // 2. 保存文件的真实路径与展示路径/用户的联系（入库）
//            file = FileConvert.INSTANCE.uploadDTOConvertTOFile(fileDTO);
            file.setFileName(fileDTO.getFileName());
            file.setFileType(file.getFileType());
            file.setMenuId(fileDTO.getMenuId());
            file.setOwner(fileDTO.getOwner());
            file.setRealPath(storeFileContext.getRealPath());
            file.setFileSize(multipartFile.getSize());
            file.setIdentifier(file.getIdentifier());
            Integer storageCode = StoreTypeEnum.getCodeByDesc(storageType);
            file.setStorageType(storageCode);
            int insert = baseMapper.insert(file);
            if (insert < 0) {
                throw new RuntimeException("上传失败");
            }
        } catch (Exception e) {
            if (!Objects.isNull(file.getRealPath())) {
                DeleteFileContext context = new DeleteFileContext()
                        .setRealFilePathList(Collections.singletonList(file.getRealPath()));
                storageService.delete(context);
            }
            throw new BusinessException(e.getMessage());
        }
        return true;
    }

    @Override
    public void deleteFile(List<Long> fileList) throws IOException {
        // 1. 查询文件的真实路径
        List<String> realPathList = baseMapper.selectBatchIds(fileList)
                .stream().map(File::getRealPath)
                .collect(Collectors.toList());
        // 2. 删除真实文件
        DeleteFileContext context = new DeleteFileContext();
        context.setRealFilePathList(realPathList);

        storageService.delete(context);

        // 3. 取消关联
        baseMapper.deleteBatchIds(fileList);
    }
}
