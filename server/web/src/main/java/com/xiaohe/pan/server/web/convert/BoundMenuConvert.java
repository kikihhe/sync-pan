package com.xiaohe.pan.server.web.convert;

import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BoundMenuConvert {
    public static final BoundMenuConvert INSTANCE = Mappers.getMapper(BoundMenuConvert.class);

    List<BoundMenuVO> boundMenuListConvertTOBoundMenuVOList(List<BoundMenu> menuList);
}
