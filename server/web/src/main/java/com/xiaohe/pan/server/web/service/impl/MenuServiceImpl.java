package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.server.web.mapper.FileMapper;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.service.MenuService;
import com.xiaohe.pan.server.web.util.ApplicationContextUtil;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Autowired
    public FileService fileService;

    @Autowired
    private FileMapper fileMapper;

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
        // TODO 删除文件时可以标记数据里的文件为删除，然后异步删真实文件，或者定时任务扫描后删除
        fileService.removeBatchByIds(fileIdList);
        return true;
    }

    @Override
    public Boolean checkNameDuplicate(Long menuId, String name) {
        LambdaQueryWrapper<Menu> lambda = new LambdaQueryWrapper<>();
        if (Objects.isNull(menuId)) {
            lambda.eq(Menu::getMenuLevel, 1);
        } else {
            lambda.eq(Menu::getParentId, menuId);
        }
        lambda.eq(Menu::getMenuName, name);
        Long count = baseMapper.selectCount(lambda);
        return count > 0;
    }
}
