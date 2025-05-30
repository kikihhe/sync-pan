package com.xiaohe.pan.server.web.controller;
import com.xiaohe.pan.server.web.util.MenuUtil;
import com.xiaohe.pan.storage.api.StoreTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.model.dto.MergeEvent;
import com.xiaohe.pan.common.util.FileUtils;
import com.xiaohe.pan.common.util.PageQuery;
import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.constants.FileConstants;
import com.xiaohe.pan.server.web.core.queue.ConflictMap;
import com.xiaohe.pan.server.web.core.queue.MergeEventQueue;
import com.xiaohe.pan.server.web.model.domain.*;
import com.xiaohe.pan.server.web.model.dto.DeleteFileDTO;
import com.xiaohe.pan.server.web.model.dto.DeletedFileQueryDTO;
import com.xiaohe.pan.server.web.model.dto.MergeChunkFileDTO;
import com.xiaohe.pan.server.web.model.dto.UpdateFileDTO;
import com.xiaohe.pan.server.web.model.dto.UploadChunkFileDTO;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.model.dto.RecycleFileDTO;
import com.xiaohe.pan.server.web.model.vo.FileChunkVO;
import com.xiaohe.pan.server.web.service.*;
import com.xiaohe.pan.server.web.util.HttpUtil;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileChunkService fileChunkService;

    @Autowired
    private MergeEventQueue mergeEventQueue;
    @Autowired
    private MenuService menuService;
    @Autowired
    private BoundMenuService boundMenuService;
    @Value("${storage.type}")
    private String storageType;
    @Autowired
    private ConflictMap conflictMap;
    @Autowired
    private FileFingerprintService fileFingerprintService;

    @Autowired
    private MenuUtil menuUtil;
    /**
     * 上传文件
     *
     * @return
     */
    @PostMapping(value = "/upload")
    public Result<String> uploadFile(UploadFileDTO fileDTO) throws IOException {
        if (Objects.isNull(fileDTO)) {
            return Result.error("请选择文件并填写参数");
        }
        Boolean nameDuplicate = fileService.checkNameDuplicate(fileDTO.getMenuId(), fileDTO.getFileName());
        if (nameDuplicate) {
            return Result.error("文件名重复!");
        }
        Long menuId = fileDTO.getMenuId();
        if (menuId != null) {
            Menu menu = menuService.getById(menuId);
            if (menu != null && menu.getBound()) {
                fileDTO.setBoundMenuId(menu.getBoundMenuId());
            }
        }
        fileDTO.setFileSize(fileDTO.getMultipartFile().getSize());
        fileDTO.setSource(1);
        fileService.uploadFile(fileDTO.getMultipartFile().getInputStream(), fileDTO);


        return Result.success("上传成功");
    }

    @PostMapping("/delete")
    public Result<String> deleteFile(@RequestBody DeleteFileDTO fileDTO) throws IOException {
        if (CollectionUtils.isEmpty(fileDTO.getFileList())) {
            return Result.success("请选择文件");
        }
//        List<File> fileList = fileService.listByIds(fileDTO.getFileList());
//        List<Long> menuIdList = fileList.stream().map(File::getMenuId).collect(Collectors.toList());
//        List<Menu> menuList = menuService.listByIds(menuIdList);
//        // 将menu.id -> menu对应为map
//        Map<Long, Menu> menuMap = menuList.stream().collect(Collectors.toMap(Menu::getId, menu -> menu));
//        for (File file : fileList) {
//            Menu menu = menuMap.get(file.getMenuId());
//            if (menu != null) {
//                if (menu.getBound()) {
//                    return Result.success("禁止在云端删除本地的文件: " + file.getFileName());
//                }
//            }
//        }

        List<Long> fileIdList = fileDTO.getFileList();
        List<File> files = fileService.listByIds(fileIdList);
        files.forEach(file -> file.setSource(1));
        fileService.updateBatchById(files);
        fileService.deleteFile(fileIdList);
        List<File> fileList = fileService.listByIds(fileDTO.getFileList());
        for (File file : fileList) {
            conflictMap.addFileConflict(file, null, 2);
        }
        return Result.success("删除成功");
    }

    @PostMapping("/updateFile")
    public Result<String> updateFileName(@RequestBody UpdateFileDTO file) throws RuntimeException {
        if (Objects.isNull(file.getId())) {
            return Result.error("修改失败");
        }
        File byId = fileService.getById(file.getId());
        if (!Objects.equals(byId.getOwner(), file.getOwner())) {
            return Result.error("权限不足");
        }
        Menu menu = menuService.getById(file.getMenuId());
        if (menu.getBound() == true) {
            return Result.success("禁止在云端修改本地的文件");
        }
        File rawFile = new File();
        BeanUtils.copyProperties(file, rawFile);
        rawFile.setSource(1);
        rawFile.setDisplayPath(FileUtils.getNewDisplayPath(byId.getDisplayPath(), file.getFileName()));
        fileService.updateById(rawFile);
        conflictMap.addFileConflict(rawFile, byId.getFileName(), 3);
        return Result.success("修改成功");
    }

    @GetMapping("/download")
    public void download(@RequestParam("id") Long id, HttpServletResponse response) throws RuntimeException, IOException {
        if (Objects.isNull(id)) {
            throw new BusinessException("请选择文件");
        }
        fileService.download(id, response);
    }

    /**
     * 文件预览
     *
     * @return
     */
    @GetMapping("/preview")
    public void previewFile(@RequestParam("fileId") Long fileId, HttpServletRequest request, HttpServletResponse response) throws RuntimeException, IOException {
        if (Objects.isNull(fileId)) {
            throw new BusinessException("请选择文件");
        }
        addCommonResponseHeader(response, request.getContentType());
        fileService.preview(fileId, response);

    }

    /**
     * 添加公共的文件读取响应头
     */
    private void addCommonResponseHeader(HttpServletResponse response, String contentTypeValue) {
        response.reset();
        HttpUtil.addCorsResponseHeaders(response);
        response.addHeader(FileConstants.CONTENT_TYPE_STR, contentTypeValue);
        response.setContentType(contentTypeValue);
    }

    @GetMapping("/checkNameDuplicate")
    public Result<String> checkNameDuplicate(@RequestParam Long menuId, @RequestParam String fileName) {
        if (menuId == 0) {
            menuId = null;
        }
        Boolean nameDuplicate = fileService.checkNameDuplicate(menuId, fileName);
        if (nameDuplicate) {
            return Result.error("同目录下文件名禁止重复: " + fileName);
        }
        return Result.success("success");
    }

    @PostMapping("/checkLargeFileExists")
    public Result<String> checkLargeFile(@RequestBody UploadChunkFileDTO chunkFileDTO) {
        FileFingerprint fingerprint = fileFingerprintService.lambdaQuery().eq(FileFingerprint::getIdentifier, chunkFileDTO.getIdentifier()).one();
        // 大文件存在，不需要分片上传
        if (!Objects.isNull(fingerprint)) {
            Menu menu = menuService.getById(chunkFileDTO.getMenuId());
            String displayPath = "/" + chunkFileDTO.getFileName();
            if (menu != null) {
                displayPath = menu.getDisplayPath() + displayPath;
            }
            File largeFile = new File();
            largeFile.setFileName(chunkFileDTO.getFileName());
            largeFile.setFileSize(chunkFileDTO.getTotalSize());
            largeFile.setRealPath(fingerprint.getRealPath());
            largeFile.setSource(1);
            largeFile.setIdentifier(fingerprint.getIdentifier());
            largeFile.setOwner(SecurityContextUtil.getCurrentUser().getId());
            largeFile.setMenuId(chunkFileDTO.getMenuId());
            largeFile.setDisplayPath(displayPath);
            largeFile.setBoundMenuId(menu == null ? null : menu.getBoundMenuId());
            largeFile.setFileType(chunkFileDTO.getFileType());
            largeFile.setStorageType(StoreTypeEnum.getCodeByDesc(storageType));
            File file = fileService.lambdaQuery().eq(File::getMenuId, largeFile.getMenuId())
                    .eq(File::getFileName, largeFile.getFileName()).one();
            if (Objects.nonNull(file)) {
                return Result.error("文件名重复");
            }
            fileService.save(largeFile);
            menuUtil.onAddFile(chunkFileDTO.getMenuId(), chunkFileDTO.getTotalSize());
            fingerprint.setReferenceCount(fingerprint.getReferenceCount() + 1);
            fileFingerprintService.updateById(fingerprint);
            return Result.success("success");
        } else {
            return Result.error("error");
        }
    }
    @PostMapping("/uploadChunk")
    public Result<Boolean> uploadChunk(UploadChunkFileDTO chunkFileDTO) throws IOException {
        File file = fileService.lambdaQuery()
                .eq(File::getIdentifier, chunkFileDTO.getIdentifier())
                .eq(File::getOwner, SecurityContextUtil.getCurrentUser().getId())
                .eq(File::getMenuId, chunkFileDTO.getMenuId())
                .eq(File::getFileName, chunkFileDTO.getFileName())
                .one();
        if (!Objects.isNull(file)) {
            throw new BusinessException("文件已存在: " + chunkFileDTO.getFileName());
        }
        return fileChunkService.uploadChunkFile(chunkFileDTO);
    }

    /**
     * 查询指定标识符的上传完成的分片列表
     *
     * @param identifier
     * @return
     */
    @GetMapping("/queryChunk")
    public Result<List<FileChunk>> queryChunk(String identifier) {
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        List<FileChunk> list = fileChunkService.getUploadedChunk(identifier, userId);
        return Result.success(list);
    }

    @PostMapping("/mergeChunk")
    public Result<String> mergeChunk(@RequestBody MergeChunkFileDTO fileDTO) throws IOException {
        fileChunkService.mergeChunk(fileDTO);
        return Result.success("success");
    }


    @PostMapping("/getDeletedFile")
    public Result<PageVO<File>> getDeletedFile(@RequestBody DeletedFileQueryDTO queryDTO) {
        Long userId = SecurityContextUtil.getCurrentUser().getId();

        PageVO<File> pageVO = fileService.getDeletedFiles(
                userId,
                queryDTO,
                StringUtils.trimToEmpty(queryDTO.getFileName())
        );
        return Result.success(pageVO);
    }

    @PostMapping("/recycle")
    public Result<Boolean> recycle(@RequestBody RecycleFileDTO recycleFileDTO) {
        if (Objects.isNull(recycleFileDTO) || Objects.isNull(recycleFileDTO.getFileId())) {
            return Result.error("文件ID不能为空");
        }
        fileService.recycleFile(recycleFileDTO.getFileId(), recycleFileDTO.getTargetMenuId());
        return Result.success(true);
    }

    /**
     * 彻底删除文件
     *
     * @param fileId
     * @return
     */
    @PostMapping("/permanentDelete")
    public Result<Boolean> permanentDelete(@RequestParam Long fileId) {
        if (Objects.isNull(fileId)) {
            return Result.error("文件不存在");
        }
        boolean r = fileService.permanentDelete(fileId);
        return Result.success(r);
    }
}
