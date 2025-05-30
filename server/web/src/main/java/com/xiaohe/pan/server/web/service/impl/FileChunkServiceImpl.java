package com.xiaohe.pan.server.web.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.FileUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.convert.FileChunkConvert;
import com.xiaohe.pan.server.web.enums.FileReferenceTypeEnum;
import com.xiaohe.pan.server.web.mapper.FileChunkMapper;
import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.FileChunk;
import com.xiaohe.pan.server.web.model.domain.FileFingerprint;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.MergeChunkFileDTO;
import com.xiaohe.pan.server.web.model.dto.UploadChunkFileDTO;
import com.xiaohe.pan.server.web.service.FileChunkService;
import com.xiaohe.pan.server.web.service.FileFingerprintService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import com.xiaohe.pan.storage.api.StorageService;
import com.xiaohe.pan.storage.api.StoreTypeEnum;
import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import com.xiaohe.pan.storage.api.context.MergeFileContext;
import com.xiaohe.pan.storage.api.context.StoreFileChunkContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileChunkServiceImpl extends ServiceImpl<FileChunkMapper, FileChunk> implements FileChunkService {

    @Resource
    private StorageService storageService;

    @Autowired
    private FileMapper fileMapper;

    @Value("${storage.type}")
    private String storageType;

    private static final ReentrantLock lock = new ReentrantLock();
    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private FileFingerprintService fileFingerprintService;

    /**
     * 上传文件分片
     *
     * @param chunkFileDTO
     * @return 是否需要合并
     */
    @Override
    public Result<Boolean> uploadChunkFile(UploadChunkFileDTO chunkFileDTO) throws IOException {
        // 0. 判断之前是否已经上传了该分片
        LambdaQueryWrapper<FileChunk> lambda = new LambdaQueryWrapper<>();
        lambda.eq(FileChunk::getChunkIdentifier, chunkFileDTO.getChunkIdentifier());
        lambda.eq(FileChunk::getIdentifier, chunkFileDTO.getIdentifier());
        FileChunk one = getOne(lambda);
        if (!Objects.isNull(one)) {
            // 已经上传过，不再上传
            log.info("分片 " + chunkFileDTO.getChunkName() + " 已经上传过，不再上传");
            return Result.success(false);
        }
        lambda.clear();
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        StoreFileChunkContext context = new StoreFileChunkContext();
        context.setUserId(userId);
        context.setChunkNumber(chunkFileDTO.getChunkNumber());
        context.setIdentifier(chunkFileDTO.getIdentifier());
        context.setFilename(chunkFileDTO.getChunkName());
        context.setTotalChunks(chunkFileDTO.getTotalChunks());
        context.setCurrentChunkSize(chunkFileDTO.getCurrentChunkSize());
        context.setInputStream(chunkFileDTO.getMultipartFile().getInputStream());
        context.setTotalSize(chunkFileDTO.getTotalSize());
        storageService.storeChunk(context);

        // 2. 保存记录
        FileChunk chunk = new FileChunk();
        BeanUtils.copyProperties(context, chunk);
        chunk.setChunkIdentifier(chunkFileDTO.getChunkIdentifier());
        chunk.setMenuId(chunkFileDTO.getMenuId());
        chunk.setFileType(chunkFileDTO.getFileType());
        chunk.setChunkSize(context.getCurrentChunkSize());
        chunk.setOwner(userId);
        Integer storeType = StoreTypeEnum.getCodeByDesc(storageType);
        chunk.setStorageType(storeType);
        baseMapper.insert(chunk);

        // 3. 判断是否需要合并
        lock.lock();
        int count = 0;
        try {
            lambda = new LambdaQueryWrapper<>();
            lambda.eq(FileChunk::getIdentifier, context.getIdentifier());
            lambda.eq(FileChunk::getOwner, userId);
            count = (int) ((long) baseMapper.selectCount(lambda));
        } finally {
            lock.unlock();
        }
        if (count == context.getTotalChunks()) {
            return Result.success(true);
        }
        return Result.success(false);
    }

    /**
     * 查询指定标识符的上传完成的分片列表
     *
     * @param identifier
     * @param userId
     * @return
     */
    @Override
    public List<FileChunk> getUploadedChunk(String identifier, Long userId) {
        LambdaQueryWrapper<FileChunk> lambda = new LambdaQueryWrapper<>();
        lambda.eq(FileChunk::getIdentifier, identifier);
        lambda.eq(FileChunk::getOwner, userId);
        lambda.lt(FileChunk::getExpirationTime, LocalDateTime.now());
        return baseMapper.selectList(lambda);
    }

    @Override
    public Boolean mergeChunk(MergeChunkFileDTO fileDTO) throws IOException {
        // 如果文件已经存在
        MergeFileContext context = new MergeFileContext();
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        List<String> chunkRealPathList = new ArrayList<>();
        // 0. 合并前查询是否已经存在该唯一标识，有可能是之前调用过该接口，但是第二部执行成功后第三步执行失败
        LambdaQueryWrapper<File> fileLambda = new LambdaQueryWrapper<>();
        fileLambda.eq(File::getIdentifier, fileDTO.getIdentifier());
        File file = fileMapper.selectOne(fileLambda);
        if (Objects.isNull(file)) {
            // 1. 查询所有分片
            LambdaQueryWrapper<FileChunk> lambda = new LambdaQueryWrapper<>();
            lambda.eq(FileChunk::getIdentifier, fileDTO.getIdentifier());
            lambda.eq(FileChunk::getOwner, userId);
            // 根据分片序号进行排序
            lambda.orderBy(true, true, FileChunk::getChunkNumber);
            List<FileChunk> chunkList = baseMapper.selectList(lambda);

            // 2. 开始合并
            if (CollectionUtils.isEmpty(chunkList)) {
                return true;
            }
            chunkRealPathList = chunkList.stream().map(FileChunk::getRealPath).collect(Collectors.toList());
            context.setIdentifier(fileDTO.getIdentifier());
            context.setUserId(userId);
            context.setRealPathList(chunkRealPathList);
            context.setFilename(fileDTO.getFileName());
            storageService.mergeFile(context);
        }
        Long menuId = fileDTO.getMenuId();
        String displayPath = fileDTO.getFileName();
        if (Objects.isNull(menuId)) {
            displayPath = "/" + displayPath;
        } else {
            Menu menu = menuMapper.selectById(menuId);
            displayPath = menu.getDisplayPath() + "/" + displayPath;
        }

        // 3. 合并后的文件入库
        File mergedFile = new File();
        mergedFile.setRealPath(context.getRealPath());
        mergedFile.setFileName(fileDTO.getFileName());
        mergedFile.setIdentifier(fileDTO.getIdentifier());
        mergedFile.setDisplayPath(displayPath);
        mergedFile.setFileType(FileUtils.getFileType(fileDTO.getFileName()));
        mergedFile.setFileSize(fileDTO.getTotalSize());
        mergedFile.setOwner(userId);
        mergedFile.setStorageType(StoreTypeEnum.getCodeByDesc(storageType));
        mergedFile.setMenuId(fileDTO.getMenuId());
        mergedFile.setSource(1);
        int insert = fileMapper.insert(mergedFile);
        if (insert <= 0) {
            return false;
        }

        // 4. 删除分片
        DeleteFileContext deleteFileContext = new DeleteFileContext()
                .setRealFilePathList(chunkRealPathList);
        storageService.delete(deleteFileContext);

        // 5. 删除分片记录
        LambdaQueryWrapper<FileChunk> lambda = new LambdaQueryWrapper<>();
        lambda.eq(FileChunk::getIdentifier, fileDTO.getIdentifier());
        baseMapper.delete(lambda);

        // 6. 添加指纹
        FileFingerprint fingerprint = new FileFingerprint();
        fingerprint.setIdentifier(fileDTO.getIdentifier());
        fingerprint.setRealPath(context.getRealPath());
        fingerprint.setReferenceCount(1);
        fingerprint.setReferenceType(FileReferenceTypeEnum.FILE.getCode());
        fileFingerprintService.save(fingerprint);
        return true;
    }
}
