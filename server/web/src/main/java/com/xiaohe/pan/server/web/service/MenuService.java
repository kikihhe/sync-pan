package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.MenuTreeDTO;

import java.util.List;


public interface MenuService extends IService<Menu> {
    public Menu addMenuByPath(Menu menu);

    public Boolean deleteMenuByPath(String displayPath);

    public List<Menu> getSubMenuByRange(Long menuId, Long userId, String name, Integer orderBy, Integer desc, Integer start, Integer count);

    public Menu getByUserAndMenuId(Long userId, Long menuId);

    public Long countByMenuId(Long menuId, Long userId, String menuName);

    public Boolean deleteMenu(Long menuId, Long userId);

    public Boolean checkNameDuplicate(Long menuId, String name);

    MenuTreeDTO batchAddMenu(MenuTreeDTO menuTreeDTO, Long userId);

    Menu getByDisplayPath(String menuPath);
}
