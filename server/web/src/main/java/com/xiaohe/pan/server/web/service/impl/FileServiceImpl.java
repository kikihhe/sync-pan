package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.PageQuery;
import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.server.web.constants.FileConstants;
import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.util.HttpUtil;
import com.xiaohe.pan.server.web.util.MenuUtil;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import com.xiaohe.pan.storage.api.StorageService;
import com.xiaohe.pan.storage.api.StoreTypeEnum;
import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import com.xiaohe.pan.storage.api.context.ReadFileContext;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
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
    public File getFileByMenuIdAndFilename(Long menuId, String filename) {
        LambdaQueryWrapper<File> lambda = new LambdaQueryWrapper<>();
        lambda.eq(File::getMenuId, menuId);
        lambda.eq(File::getFileName, filename);
        return baseMapper.selectOne(lambda);
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
    public Boolean uploadFile(InputStream inputStream, UploadFileDTO fileDTO) throws IOException {
        File file = new File();
        try {
            // 1. 保存真实文件
            StoreFileContext storeFileContext = new StoreFileContext()
                    .setFilename(fileDTO.getFileName())
                    .setTotalSize(fileDTO.getFileSize())
                    .setInputStream(inputStream);
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
            file.setFileSize(fileDTO.getFileSize());
            file.setIdentifier(fileDTO.getIdentifier());
            file.setSource(fileDTO.getSource());
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
//        // 1. 查询文件的真实路径
        List<File> files = baseMapper.selectBatchIds(fileList);
//        List<String> realPathList = files
//                .stream().map(File::getRealPath)
//                .collect(Collectors.toList());
//        // 2. 删除真实文件
//        DeleteFileContext context = new DeleteFileContext();
//        context.setRealFilePathList(realPathList);
//
//        storageService.delete(context);

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

    @Override
    public void download(Long id, HttpServletResponse response) throws RuntimeException, IOException {
        File file = getById(id);
        if (Objects.isNull(file)) {
            throw new BusinessException("文件不存在");
        }
        // 1. 设置响应的属性
        addCommonResponseHeader(response, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        addDownloadAttribute(response, file);

        // 2. 将数据流读到 response 中
        ReadFileContext context = new ReadFileContext();
        context.setRealPath(file.getRealPath());
        context.setOutputStream(response.getOutputStream());
        storageService.readFile(context);
    }

    @Override
    public void recycleFile(Long fileId, Long targetMenuId) {
        File file = baseMapper.getDeletedFileById(fileId);
        if (Objects.isNull(file)) {
            throw new BusinessException("文件不存在");
        }
        if (!Objects.isNull(targetMenuId)) {
            Menu menu = menuMapper.selectById(targetMenuId);
            if (Objects.isNull(menu)) {
                throw new BusinessException("目录不存在");
            }
        }
        if (!Objects.equals(file.getOwner(), SecurityContextUtil.getCurrentUser().getId())) {
            throw new BusinessException("权限不足");
        }

        // 更新文件的目录ID
        baseMapper.updateForRecycle(fileId, targetMenuId);

        // 更新目标目录大小
        menuUtil.onAddFile(targetMenuId, file.getFileSize());
    }

    @Override
    public PageVO<File> getDeletedFiles(Long userId, PageQuery pageQuery, String fileName) {
        int pageSize = pageQuery.getPageSize();
        int pageNum = pageQuery.getPageNum();
        int offset = pageNum * pageSize;

        List<File> records = baseMapper.selectDeletedFiles(
                userId,
                fileName,
                offset,
                pageSize
        );

        Long total = baseMapper.countDeletedFiles(userId, fileName);

        PageVO<File> pageVO = new PageVO<>();
        pageVO.setTotal(total.intValue())
                .setPageNum(pageNum + 1)
                .setPageSize(pageSize)
                .setRecords(records);
        return pageVO;
    }

    @Override
    public boolean permanentDelete(Long fileId) throws BusinessException {
        File file = getById(fileId);
        if (Objects.isNull(file)) {
            throw new BusinessException("文件不存在");
        }
        int result = baseMapper.permanentDeleteById(fileId);
        if (result < 1) {
            throw new BusinessException("删除失败");
        }
        DeleteFileContext context = new DeleteFileContext();
        context.setRealFilePathList(Collections.singletonList(file.getRealPath()));
        try {
            storageService.delete(context);
        } catch (Exception e) {
            throw new BusinessException("删除失败");
        }
        return true;
    }

    @Override
    public Boolean deleteByDisplayPath(String displayPath) throws IOException {
        File file = getByDisplayPath(displayPath);
        deleteFile(Collections.singletonList(file.getId()));
        return true;
    }

    @Override
    public List<File> selectFilesDeletedBefore30Days() {
        return baseMapper.selectFilesDeletedBefore30Days();
    }

    public File getByDisplayPath(String displayPath) {
        LambdaQueryWrapper<File> lambda = new LambdaQueryWrapper<>();
        lambda.eq(File::getDisplayPath, displayPath);
        return baseMapper.selectOne(lambda);
    }
    /**
     * 添加公共的文件读取响应头
     *
     * @param response
     * @param contentTypeValue
     */
    private void addCommonResponseHeader(HttpServletResponse response, String contentTypeValue) {
        response.reset();
        HttpUtil.addCorsResponseHeaders(response);
        response.addHeader(FileConstants.CONTENT_TYPE_STR, contentTypeValue);
        response.setContentType(contentTypeValue);
    }
    /**
     * 添加文件下载的属性信息
     */
    private void addDownloadAttribute(HttpServletResponse response, File file) {
        try {
            response.addHeader(FileConstants.CONTENT_DISPOSITION_STR,
                        FileConstants.CONTENT_DISPOSITION_VALUE_PREFIX_STR + new String(file.getFileName().getBytes(FileConstants.GB2312_STR),
                            FileConstants.IOS_8859_1_STR));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new BusinessException("文件下载失败");
        }
        response.setContentLengthLong(file.getFileSize());
    }
}
