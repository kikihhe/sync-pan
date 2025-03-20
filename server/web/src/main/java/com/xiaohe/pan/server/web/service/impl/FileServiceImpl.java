package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.util.MenuUtil;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import com.xiaohe.pan.storage.api.StorageService;
import com.xiaohe.pan.storage.api.StoreTypeEnum;
import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import com.xiaohe.pan.storage.api.context.ReadFileContext;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Resource
    private StorageService storageService;

    @Resource
    private MenuMapper menuMapper;

    @Value("${storage.type}")
    private String storageType;

    @Resource
    private MenuUtil menuUtil;

    /**
     * 获取指定目录下的文件
     */
    @Override
    public List<File> getSubFileByRange(Long menuId, Long userId, String name, Integer orderBy, Integer desc, Integer start, Integer count) {
        return baseMapper.selectSubFileByRange(menuId, userId, name, orderBy, desc, start, count);
    }

    @Override
    public Long countByMenuId(Long menuId, Long userId, String fileName) {
        LambdaQueryWrapper<File> lambda = new LambdaQueryWrapper<>();
        if (Objects.isNull(menuId)) {
            lambda.isNull(File::getMenuId);
        } else {
            lambda.eq(File::getMenuId, menuId);
        }
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
            if (!Objects.isNull(fileDTO.getMenuId())) {
                Menu menu = menuMapper.selectById(fileDTO.getMenuId());
                file.setDisplayPath(menu.getDisplayPath() + "/" + fileDTO.getFileName());
            } else {
                file.setDisplayPath("/" + file.getFileName());
            }
            file.setFileName(fileDTO.getFileName());
            file.setFileType(fileDTO.getFileType());
            file.setMenuId(fileDTO.getMenuId());
            file.setOwner(SecurityContextUtil.getCurrentUser().getId());
            file.setRealPath(storeFileContext.getRealPath());
            file.setFileSize(multipartFile.getSize());
            file.setIdentifier(fileDTO.getIdentifier());
            Integer storageCode = StoreTypeEnum.getCodeByDesc(storageType);
            file.setStorageType(storageCode);
            int insert = baseMapper.insert(file);
            if (insert < 0) {
                throw new RuntimeException("上传失败");
            }
            menuUtil.onAddFile(fileDTO.getMenuId(), file.getFileSize());
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
        List<File> files = baseMapper.selectBatchIds(fileList);
        List<String> realPathList = files
                .stream().map(File::getRealPath)
                .collect(Collectors.toList());
        // 2. 删除真实文件
        DeleteFileContext context = new DeleteFileContext();
        context.setRealFilePathList(realPathList);

        storageService.delete(context);

        // 3. 取消关联
        baseMapper.deleteBatchIds(fileList);
        // 4. 计算大小
        files = files.stream().filter(file -> Objects.nonNull(file.getMenuId())).collect(Collectors.toList());
        Map<Long, Long> collect = files.stream().collect(Collectors.groupingBy(File::getMenuId, Collectors.summingLong(File::getFileSize)));
        collect.forEach((menuId, size) -> {
            menuUtil.onDeleteFile(menuId, size);
        });
    }


    @Override
    public Boolean checkNameDuplicate(Long menuId, String name) {
        LambdaQueryWrapper<File> lambda = new LambdaQueryWrapper<>();
        lambda.eq(File::getMenuId, menuId);
        lambda.eq(File::getFileName, name);
        Long count = baseMapper.selectCount(lambda);
        return count > 0;
    }

    @Override
    public void preview(Long fileId, HttpServletResponse response) throws IOException {
        File file = baseMapper.selectById(fileId);
        if (Objects.isNull(file)) {
            throw new BusinessException("文件不存在");
        }
        if (!Objects.equals(file.getOwner(), SecurityContextUtil.getCurrentUser().getId())) {
            throw new BusinessException("权限不足");
        }
        // 真实路径
        String realPath = file.getRealPath();
        // 将真实文件内容读取到 response.outputStream 中
        ReadFileContext readFileContext = new ReadFileContext()
                .setRealPath(realPath)
                .setOutputStream(response.getOutputStream());
        storageService.readFile(readFileContext);
    }
}
