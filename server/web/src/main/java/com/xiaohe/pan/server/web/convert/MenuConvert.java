package com.xiaohe.pan.server.web.convert;

import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.MenuTreeDTO;
import com.xiaohe.pan.server.web.model.vo.MenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MenuConvert {
    MenuConvert INSTANCE = Mappers.getMapper(MenuConvert.class);

    MenuVO menuConvertTOMenuVO(Menu menu);

    /**
     * 树节点转为Menu
     * @param menuTreeDTO
     * @return
     */
    Menu menuTreeTOMenu(MenuTreeDTO menuTreeDTO);
}
