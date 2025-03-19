package com.xiaohe.pan.server.web.convert;


import com.xiaohe.pan.server.web.model.domain.FileChunk;
import com.xiaohe.pan.storage.api.context.StoreFileChunkContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FileChunkConvert {

    public static final FileChunkConvert INSTANCE = Mappers.getMapper(FileChunkConvert.class);

    @Mappings({
            @Mapping(source = "currentChunkSize", target = "chunkSize")
    })
    FileChunk contextToChunk(StoreFileChunkContext context);
}
