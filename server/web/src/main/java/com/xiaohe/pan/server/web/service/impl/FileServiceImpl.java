package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.FileUtils;
import com.xiaohe.pan.common.util.PageQuery;
import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.server.web.constants.FileConstants;
import com.xiaohe.pan.server.web.enums.FileReferenceTypeEnum;
import com.xiaohe.pan.server.web.mapper.FileFingerprintMapper;
import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.FileFingerprint;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.domain.User;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.service.FileFingerprintService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
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
    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileFingerprintMapper fileFingerprintMapper;

    @Autowired
    private FileFingerprintService fileFingerprintService;

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
            // 保存真实文件
            if (inputStream == null) {
                inputStream = new ByteArrayInputStream(fileDTO.getData(), 0, fileDTO.getData().length);
            }
            // 如果前端没有指定，可能是同步服务在添加文件
            String identifier = fileDTO.getIdentifier();
            if (StringUtils.isEmpty(identifier)) {
                fileDTO.setIdentifier(FileUtils.calculateFileMD5(fileDTO.getData()));
            }
            StoreFileContext storeFileContext = new StoreFileContext()
                    .setFilename(fileDTO.getFileName())
                    .setTotalSize(fileDTO.getFileSize())
                    .setInputStream(inputStream);
            FileFingerprint existsFileFingerprint = fileFingerprintService.lambdaQuery()
                    .eq(FileFingerprint::getIdentifier, fileDTO.getIdentifier())
                    .eq(FileFingerprint::getReferenceType, FileReferenceTypeEnum.FILE.getCode())
                    .one();
            if (existsFileFingerprint == null) {
                // 文件不存在调用文件服务，文件的真实路径会保存在 context 中
                storageService.store(storeFileContext);
            } else {
                // 文件已存在, 不用真实保存，添加引用即可
                storeFileContext.setRealPath(existsFileFingerprint.getRealPath());
                existsFileFingerprint.setReferenceCount(existsFileFingerprint.getReferenceCount() + 1);
                fileFingerprintService.updateById(existsFileFingerprint);
            }

            String realPath = storeFileContext.getRealPath();
            // 2. 保存文件的真实路径与展示路径/用户的联系（入库）
//            file = FileConvert.INSTANCE.uploadDTOConvertTOFile(fileDTO);
            Menu menu = new Menu();
            if (!Objects.isNull(fileDTO.getMenuId())) {
                menu = menuMapper.selectById(fileDTO.getMenuId());
                file.setDisplayPath(menu.getDisplayPath() + "/" + fileDTO.getFileName());
            } else {
                file.setDisplayPath("/" + fileDTO.getFileName());
            }
            Long userId = SecurityContextUtil.getCurrentUser() == null ? menu.getOwner() : SecurityContextUtil.getCurrentUser().getId();
            file.setFileName(fileDTO.getFileName());
            file.setFileType(fileDTO.getFileType());
            file.setMenuId(fileDTO.getMenuId());
            file.setOwner(userId);

            file.setRealPath(realPath);
            file.setFileSize(fileDTO.getFileSize());
            file.setIdentifier(identifier);
            file.setSource(fileDTO.getSource() == null ? 1 : fileDTO.getSource());
            file.setBoundMenuId(fileDTO.getBoundMenuId());
            Integer storageCode = StoreTypeEnum.getCodeByDesc(storageType);
            file.setStorageType(storageCode);
            int insert = baseMapper.insert(file);
            if (insert < 0) {
                throw new RuntimeException("上传失败");
            }
            // 修改文件所属目录的大小
            menuUtil.onAddFile(fileDTO.getMenuId(), file.getFileSize());
            // 将文件的真实路径记录到 file_fingerprint 中
            if (existsFileFingerprint == null) {
                FileFingerprint fileFingerprint = new FileFingerprint();
                fileFingerprint.setIdentifier(file.getIdentifier());
                fileFingerprint.setRealPath(file.getRealPath());
                fileFingerprint.setReferenceType(FileReferenceTypeEnum.FILE.getCode());
                fileFingerprint.setReferenceCount(1);
                fileFingerprintMapper.insert(fileFingerprint);
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
    public List<File> selectAllFilesByMenuId(Long menuId) {
        return baseMapper.selectAllFilesByMenuId(menuId);
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
        File file = fileMapper.getDeletedFileById(fileId);
        if (Objects.isNull(file)) {
            throw new BusinessException("文件不存在");
        }
        int result = baseMapper.permanentDeleteById(fileId);
        if (result < 1) {
            throw new BusinessException("删除失败");
        }
        // 减小文件指纹
        FileFingerprint one = fileFingerprintService.lambdaQuery()
                .eq(FileFingerprint::getIdentifier, file.getIdentifier())
//                不应该指定文件类型，因为彻底删除也有可能作用于大文件
//                .eq(FileFingerprint::getReferenceType, FileReferenceTypeEnum.FILE.getCode())
                .one();
        if (one != null) {
            // 减小到0时彻底删除
            one.setReferenceCount(one.getReferenceCount() - 1);
            fileFingerprintService.updateById(one);
            if (one.getReferenceCount() == 0) {
                DeleteFileContext context = new DeleteFileContext();
                context.setRealFilePathList(Collections.singletonList(file.getRealPath()));
                try {
                    storageService.delete(context);
                } catch (Exception e) {
                    throw new BusinessException("删除失败");
                }
            }
        }
        return true;
    }

    @Override
    public Boolean deleteByDisplayPath(String displayPath) throws IOException {
        File file = getByDisplayPath(displayPath);
        if (file == null) {
            return false;
        }
        deleteFile(Collections.singletonList(file.getId()));
        return true;
    }

    @Override
    public List<File> selectFilesDeletedBefore30Days() {
        return baseMapper.selectFilesDeletedBefore30Days();
    }

    @Override
    public List<File> getAllSubFile(Long menuId, List<File> result) {
        if (Objects.isNull(menuId)) {
            return null;
        }
        List<File> subFileList = lambdaQuery().eq(File::getMenuId, menuId).list();
        if (subFileList.isEmpty()) {
            return null;
        }
        result.addAll(subFileList);

        for (File subFile : subFileList) {
            getAllSubFile(subFile.getId(), result);
        }
        return null;
    }

    @Override
    public List<File> getSubFileByMenuList(List<Long> menuIdList) {
        LambdaQueryWrapper<File> lambda = new LambdaQueryWrapper<>();
        lambda.in(File::getMenuId, menuIdList);
        return baseMapper.selectList(lambda);
    }

    public File getByDisplayPath(String displayPath) {
        LambdaQueryWrapper<File> lambda = new LambdaQueryWrapper<>();
        lambda.eq(File::getDisplayPath, displayPath);
        return baseMapper.selectOne(lambda);
    }

    @Override
    public byte[] readFile(File file) throws IOException {
        ReadFileContext context = new ReadFileContext();
        // 创建字节数组输出流
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        context.setRealPath(file.getRealPath());
        // 设置输出流
        context.setOutputStream(bos);

        storageService.readFile(context);

        return bos.toByteArray();
    }

    @Override
    public boolean recycle(File file) {
        int result = fileMapper.recycle(file);
        return result > 0;
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
