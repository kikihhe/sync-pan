package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.convert.PageConvert;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {
    public List<Menu> getSubMenuByRange(Long menuId, Long userId, Integer start, Integer count) {
        return baseMapper.selectSubMenuByRange(menuId, userId, start, count);
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
    public Long countByMenuId(Long menuId, Long userId) {
        LambdaQueryWrapper<Menu> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Menu::getOwner, userId);
        if (Objects.isNull(menuId)) {
            lambda.eq(Menu::getMenuLevel, 1);
        } else {
            lambda.eq(Menu::getParentId, menuId);
        }
        return count(lambda);
    }
}
