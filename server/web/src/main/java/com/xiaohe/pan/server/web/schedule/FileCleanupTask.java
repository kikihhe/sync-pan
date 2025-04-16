package com.xiaohe.pan.server.web.schedule;

import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.storage.api.StorageService;
import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class FileCleanupTask {

    @Resource
    private FileService fileService;

    /**
     * 每天凌晨1点执行清理任务
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupExpiredFiles() {
        log.info("开始执行文件清理任务");
        try {
            // 1. 查询30天前被删除的文件
            List<File> expiredFiles = fileService.selectFilesDeletedBefore30Days();
            if (expiredFiles == null || expiredFiles.isEmpty()) {
                log.info("没有需要清理的文件");
                return;
            }

            // 2. 遍历文件列表，删除物理文件和数据库记录
            for (File file : expiredFiles) {
                try {
                    fileService.permanentDelete(file.getId());
                } catch (Exception e) {
                    log.error("清理文件失败: {}, 错误: {}", file.getFileName(), e.getMessage());
                }
            }
            log.info("文件清理任务完成，共清理{}个文件", expiredFiles.size());
        } catch (Exception e) {
            log.error("执行文件清理任务时发生错误", e);
        }
    }
}