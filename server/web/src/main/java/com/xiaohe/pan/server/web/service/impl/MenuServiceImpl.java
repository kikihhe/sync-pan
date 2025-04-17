package com.xiaohe.pan.server.web.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.server.web.convert.MenuConvert;
import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.MenuTreeDTO;
import com.xiaohe.pan.server.web.model.vo.ConflictVO;
import com.xiaohe.pan.server.web.model.vo.FileConflictVO;
import com.xiaohe.pan.server.web.model.vo.MenuConflictVO;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.service.MenuService;
import com.xiaohe.pan.server.web.util.ApplicationContextUtil;
import com.xiaohe.pan.server.web.util.MenuUtil;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import com.xiaohe.pan.server.web.util.Snowflake;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Autowired
    public FileService fileService;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private MenuUtil menuUtil;

    @Override
    public List<Menu> getSubMenuByRange(Long menuId, Long userId, String name, Integer orderBy, Integer desc, Integer start, Integer count) {
        return baseMapper.selectSubMenuByRange(menuId, userId, name, orderBy, desc, start, count);
    }

    @Override
    public Menu getByUserAndMenuId(Long userId, Long menuId) {
        LambdaQueryWrapper<Menu> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Menu::getOwner, userId);
        lambda.eq(Menu::getId, menuId);
        return baseMapper.selectOne(lambda);
    }

    /**
     * 查询指定目录的子目录数量
     * @param menuId
     * @param userId
     * @return
     */
    public Long countByMenuId(Long menuId, Long userId, String menuName) {
        LambdaQueryWrapper<Menu> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Menu::getOwner, userId);
        if (Objects.isNull(menuId)) {
            lambda.eq(Menu::getMenuLevel, 1);
        } else {
            lambda.eq(Menu::getParentId, menuId);
        }
        return count(lambda);
    }

    /**
     * 删除目录，以及子目录/子文件
     * @param menuId
     * @return
     */
    @Override
    @Transactional
    public Boolean deleteMenu(Long menuId, Long userId) {
        // 1. 查询出子目录
        LambdaQueryWrapper<Menu> menuLambda = new LambdaQueryWrapper<>();
        // 如果为空，说明要删除一级目录
        if (Objects.isNull(menuId)) {
            menuLambda.eq(Menu::getMenuLevel, 1);
        } else {
            menuLambda.eq(Menu::getParentId, menuId);
        }
        int delete = baseMapper.deleteById(menuId);
        if (delete <= 0) {
            throw new BusinessException("删除失败!");
        }
        menuLambda.eq(Menu::getOwner, userId);
        List<Menu> menuList = baseMapper.selectList(menuLambda);
        if (!CollectionUtils.isEmpty(menuList)) {
            // 2. 递归删除子目录
            MenuService menuService = (MenuService) AopContext.currentProxy();
            menuList.forEach(menu -> {
                menuService.deleteMenu(menu.getId(), userId);
            });
        }

        // 3. 查询出子文件并且删除
        LambdaQueryWrapper<File> fileLambda = new LambdaQueryWrapper<>();
        fileLambda.eq(File::getOwner, userId);
        fileLambda.eq(File::getMenuId, menuId);
        List<Long> fileIdList = fileMapper.selectList(fileLambda)
                .stream()
                .map(File::getId)
                .collect(Collectors.toList());
        fileService.removeBatchByIds(fileIdList);
        return true;
    }

    @Override
    public Boolean checkNameDuplicate(Long parentId, String name) {
        LambdaQueryWrapper<Menu> lambda = new LambdaQueryWrapper<>();
        if (Objects.isNull(parentId)) {
            lambda.eq(Menu::getMenuLevel, 1);
        } else {
            lambda.eq(Menu::getParentId, parentId);
        }
        lambda.eq(Menu::getMenuName, name);
        Long count = baseMapper.selectCount(lambda);
        return count > 0;
    }

    public Menu getByDisplayPath(String displayPath) {
        if (!StringUtils.hasText(displayPath)) {
            throw new BusinessException("路径不合法!");
        }
        LambdaQueryWrapper<Menu> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Menu::getDisplayPath, displayPath);
        return getOne(lambda);
    }

    /**
     * 检查冲突
     */
    @Override
    public List<ConflictVO> checkConflict(Menu menu) {
        List<ConflictVO> result = new ArrayList<>(2);

        // 云端冲突（source=1）
        ConflictVO cloudConflict = new ConflictVO();
        cloudConflict.setFileConflictVOList(getFileConflicts(menu.getId(), 1));
        cloudConflict.setMenuConflictVOList(getMenuConflicts(menu.getId(), 1));
        result.add(cloudConflict);

        // 本地冲突（source=2）
        ConflictVO localConflict = new ConflictVO();
        localConflict.setFileConflictVOList(getFileConflicts(menu.getId(), 2));
        localConflict.setMenuConflictVOList(getMenuConflicts(menu.getId(), 2));
        result.add(localConflict);

        return result;
    }
    private List<FileConflictVO> getFileConflicts(Long menuId, Integer source) {
        List<File> files = fileService.lambdaQuery()
                .eq(File::getMenuId, menuId)
                .eq(File::getSource, source)
                .list();

        return files.stream()
                .map(f -> new FileConflictVO().setFile(f).setType(1)) // 根据实际业务设置类型
                .collect(Collectors.toList());
    }

    private List<MenuConflictVO> getMenuConflicts(Long menuId, Integer source) {
        List<Menu> menus = this.lambdaQuery()
                .eq(Menu::getParentId, menuId)
                .eq(Menu::getSource, source)
                .list();

        return menus.stream()
                .map(m -> new MenuConflictVO().setMenu(m).setType(1)) // 根据实际业务设置类型
                .collect(Collectors.toList());
    }
    /**
     * 根据树形批量创建目录
     * @param menuTreeDTO
     * @param userId
     * @return
     */
    @Override
    public MenuTreeDTO batchAddMenu(MenuTreeDTO menuTreeDTO, Long userId) {
        // 如果在根目录上传文件夹
        if (Objects.isNull(menuTreeDTO.getParentId())) {
            menuTreeDTO.setMenuLevel(1);
        }
        // 1. 给树目录生成id，并添加 parentId
        generateIds(menuTreeDTO, menuTreeDTO.getParentId());
        Menu parentMenu = getByUserAndMenuId(userId, menuTreeDTO.getParentId());
        // 2. 转为 Menu 插入数据库
        List<Menu> menuList = new ArrayList<>();
        menuTreeTOMenuList(parentMenu, menuTreeDTO, menuList);
        saveBatch(menuList);
        // 3. 异步在 redis 创建目录
        for (Menu m : menuList) {
            menuUtil.onAddMenu(m.getId(), m.getParentId());
        }
        // 4. 返回
        return menuTreeDTO;
    }

    private void menuTreeTOMenuList(Menu parent, MenuTreeDTO menuTreeDTO, List<Menu> list) {
        Menu menu = MenuConvert.INSTANCE.menuTreeTOMenu(menuTreeDTO);
        menu.setOwner(SecurityContextUtil.getCurrentUserId());
        list.add(menu);
        if (CollectionUtils.isEmpty(menuTreeDTO.getChildren())) {
            return;
        }
        for (MenuTreeDTO child : menuTreeDTO.getChildren()) {
            menuTreeTOMenuList(parent, child, list);
        }
        for (Menu m : list) {
            m.setSource(1);
            m.setBound(parent.getBound());
        }
    }

    private void generateIds(MenuTreeDTO menuTreeDTO, Long parentId) {
        menuTreeDTO.setId(generateUniqueId());
        menuTreeDTO.setParentId(parentId);
        if (!CollectionUtils.isEmpty(menuTreeDTO.getChildren())) {
            for (MenuTreeDTO child : menuTreeDTO.getChildren()) {
                generateIds(child, menuTreeDTO.getId());
            }
        }
    }

    private Long generateUniqueId() {
        return Snowflake.INSTANCE.nextId();
    }

    @Override
    @Transactional
    public Menu addMenuByPath(Menu menu) {
        Long userId = menu.getOwner();
        Menu current = new Menu();
        String path = menu.getDisplayPath();
        String[] parts = path.split("/");
        if (parts.length == 0) {
            throw new BusinessException("路径不合法");
        }
        Long currentParentId = null;
        int currentLevel = 1;
        String rollingPath = "";
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            String menuName = part;
            part = rollingPath + "/" + part;
            Menu byDisplayPath = getByDisplayPath(part);
            if (!Objects.isNull(byDisplayPath)) {
                currentParentId = byDisplayPath.getId();
                currentLevel++;
                rollingPath = part;
                continue;
            }
            current = new Menu()
                    .setMenuName(menuName)
                    .setParentId(currentParentId)
                    .setMenuLevel(currentLevel)
                    .setDisplayPath(part)
                    .setOwner(userId)
                    .setSource(menu.getSource());
            boolean save = save(current);
            if (!save) {
                throw new BusinessException("目录添加失败: " + part);
            }
            currentParentId = current.getId();
            currentLevel++;
            rollingPath = part;
        }
        return current;
    }

    @Override
    public Boolean deleteMenuByPath(String displayPath) {
        Menu menu = getByDisplayPath(displayPath);
        if (Objects.isNull(menu)) {
            throw new BusinessException("目录不存在");
        }
        return removeById(menu.getId());
    }
}
