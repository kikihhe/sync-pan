package com.xiaohe.pan.server.web.convert;

import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FileConvert {
    FileConvert INSTANCE = Mappers.getMapper(FileConvert.class);


    File uploadDTOConvertTOFile(UploadFileDTO uploadFileDTO);
}
