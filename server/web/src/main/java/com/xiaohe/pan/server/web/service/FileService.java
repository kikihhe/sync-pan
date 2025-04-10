package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaohe.pan.common.util.PageQuery;
import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface FileService extends IService<File> {

    public List<File> getSubFileByRange(Long menuId, Long userId, String name, Integer orderBy, Integer desc, Integer start, Integer count);

    public Long countByMenuId(Long menuId, Long userId, String fileName);

    public Boolean uploadFile(MultipartFile multipartFile, UploadFileDTO fileDTO) throws IOException;

    public void deleteFile(List<Long> fileList) throws IOException;

    public Boolean checkNameDuplicate(Long menuId, String name);

    void preview(Long fileId, HttpServletResponse response) throws IOException;

    void download(Long id, HttpServletResponse response) throws RuntimeException, IOException;

    public PageVO<File> getDeletedFiles(Long userId, PageQuery pageQuery, String fileName);

    void recycleFile(Long fileId, Long targetMenuId);
}
