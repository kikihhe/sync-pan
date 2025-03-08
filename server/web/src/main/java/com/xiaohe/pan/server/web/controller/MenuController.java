package com.xiaohe.pan.server.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.PageVO;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.convert.MenuConvert;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.SubMenuListDTO;
import com.xiaohe.pan.server.web.model.vo.MenuVO;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.service.MenuService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private FileService fileService;

    /**
     * 获取指定目录的子目录
     * 若 menuId 为空，则获取指定用户的所有一级目录
     * @return
     */
    @PostMapping("/getSubMenuList")
    public Result<MenuVO> getSubMenuList(@RequestBody SubMenuListDTO menuDTO) throws Exception {
        Long currentUserId = SecurityContextUtil.getCurrentUser().getId();
        // 首先验证当前目录是否为当前用户的
        if (!Objects.isNull(menuDTO.getMenuId())) {
            Menu menu = menuService.getByUserAndMenuId(currentUserId, menuDTO.getMenuId());
            if (Objects.isNull(menu)) {
                return Result.error("请不要操作别人的数据");
            }
        }
        // 分页参数处理
        int pageSize = menuDTO.getPageSize();
        int pageNum = menuDTO.getPageNum();
        int start = pageNum * pageSize;
        Long menuTotal = menuService.countByMenuId(menuDTO.getMenuId(), currentUserId);
        Long fileTotal = fileService.countByMenuId(menuDTO.getMenuId(), currentUserId);
        // 计算分页分布
        List<Menu> menuList = Collections.emptyList();
        List<File> fileList = Collections.emptyList();

        // 当前页在目录范围内
        if (start < menuTotal) {
            int dirCount = (int) Math.min(menuTotal - start, pageSize);
            menuList = menuService.getSubMenuByRange(menuDTO.getMenuId(), currentUserId, start, dirCount);
            // 需要补充文件数据
            if (menuList.size() < pageSize) {
                int fileCount = pageSize - menuList.size();
                // 从第0页开始查
                fileList = fileService.getSubFileByRange(menuDTO.getMenuId(), currentUserId, 0, fileCount);
            }
        } else {
            // 2. 当前页在文件范围内, 只需要获取文件数据
            int fileStart = (int) (start - menuTotal);
            int fileCount = Math.min((int) (fileTotal - fileStart), pageSize);
            fileList = fileService.getSubFileByRange(menuDTO.getMenuId(), currentUserId, fileStart, fileCount);
        }

        MenuVO menuVO = new MenuVO()
            .setSubMenuList(menuList)
            .setSubFileList(fileList)
            .setPageNum(menuDTO.getPageNum() + 1)
            .setPageSize(menuDTO.getPageSize())
            .setTotal((int) (fileTotal + menuTotal));

        return Result.success(menuVO);
    }

    /**
     * 添加目录
     * menuLevel 需要前端传入
     * @param menu
     * @return
     */
    @PostMapping("/addMenu")
    public Result<String> addMenu(@RequestBody Menu menu) {
        menu.setOwner(SecurityContextUtil.getCurrentUser().getId());
        boolean save = menuService.save(menu);
        if (!save) {
            return Result.error("目录添加失败，请稍后再试");
        }
        return Result.success("目录添加成功");
    }

    @PostMapping("/updateMenu")
    public Result<String> updateMenu(@RequestBody Menu menu) throws RuntimeException {
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        if (!Objects.equals(userId, menu.getOwner())) {
            throw new BusinessException("权限不足");
        }
        boolean b = menuService.updateById(menu);
        if (!b) {
            return Result.error("修改失败");
        }
        return Result.success("修改成功");
    }

    /**
     * 删除目录，同步删除它的子目录以及文件
     * @param menuId
     * @return
     */
    @PostMapping("/deleteMenu")
    public Result<String> deleteMenu(@RequestParam Long menuId) throws RuntimeException {
        Long userId = SecurityContextUtil.getCurrentUser().getId();

        Menu menu = menuService.getByUserAndMenuId(userId, menuId);
        if (Objects.isNull(menu)) {
            throw new BusinessException("目录不存在");
        }
        menuService.deleteMenu(menuId, userId);
        return Result.success("删除成功");
    }

}
