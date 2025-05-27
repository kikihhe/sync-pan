package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.MenuTreeDTO;
import com.xiaohe.pan.server.web.model.vo.ConflictVO;

import java.util.List;


public interface MenuService extends IService<Menu> {
    public Menu addMenuByPath(Menu menu);

    public Boolean deleteMenuByPath(String displayPath);

    public List<Menu> getAllSubMenu(Long menuId, List<Menu> result);

    public List<Menu> getSubMenuByRange(Long menuId, Long userId, String name, Integer orderBy, Integer desc, Integer start, Integer count);

    public Menu getByUserAndMenuId(Long userId, Long menuId);

    public Long countByMenuId(Long menuId, Long userId, String menuName);

    public Boolean deleteMenu(Long menuId, Long userId);

    public Boolean checkNameDuplicate(Long menuId, String name);

    MenuTreeDTO batchAddMenu(MenuTreeDTO menuTreeDTO, Long userId);

    Menu getByDisplayPath(String menuPath);

    List<ConflictVO> checkConflict(Menu menu);

    List<Menu> selectAllMenusByMenuId(Long currentMenuId);

    boolean resolve(Menu menu);
}
