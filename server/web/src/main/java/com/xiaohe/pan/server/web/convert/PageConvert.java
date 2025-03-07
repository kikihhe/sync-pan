package com.xiaohe.pan.server.web.convert;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaohe.pan.common.util.PageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PageConvert {
    PageConvert INSTANCE = Mappers.getMapper(PageConvert.class);


    /**
     * mybatis-plus 的分页 page 转为自己的分页 page
     * @param mybatisPage
     * @return
     */
    @Mappings({
            @Mapping(source = "size", target = "pageSize"),
            @Mapping(source = "current", target = "pageNum")
    })
    <T> PageVO<T> mybatisPageConvertTOPageVO(Page<T> mybatisPage);
}
