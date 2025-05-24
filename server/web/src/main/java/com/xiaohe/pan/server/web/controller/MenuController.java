package com.xiaohe.pan.server.web.controller;

import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.FileUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.convert.MenuConvert;
import com.xiaohe.pan.server.web.core.queue.ConflictMap;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.MenuTreeDTO;
import com.xiaohe.pan.server.web.model.dto.SubMenuListDTO;
import com.xiaohe.pan.server.web.model.vo.ConflictVO;
import com.xiaohe.pan.server.web.model.vo.FileAndMenuListVO;
import com.xiaohe.pan.server.web.model.vo.MenuDetailVO;
import com.xiaohe.pan.server.web.model.vo.MenuVO;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.service.MenuService;
import com.xiaohe.pan.server.web.util.MenuUtil;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private FileService fileService;

    @Autowired
    private MenuUtil menuUtil;

    @Autowired
    private ConflictMap conflictMap;


    /**
     * 添加目录
     * menuLevel 需要前端传入
     * @param menu
     * @return
     */
    @PostMapping("/addMenu")
    public Result<String> addMenu(@RequestBody Menu menu) {
        menu.setOwner(SecurityContextUtil.getCurrentUser().getId());
        menu.setSource(1);
        Boolean nameDuplicate = menuService.checkNameDuplicate(menu.getParentId(), menu.getMenuName());
        if (nameDuplicate) {
            return Result.error("目录名重复!");
        }
        Menu parentMenu = new Menu();
        if (!Objects.isNull(menu.getParentId())) {
            parentMenu = menuService.getById(menu.getParentId());
            menu.setDisplayPath(parentMenu.getDisplayPath() + "/" + menu.getMenuName());
            menu.setBound(parentMenu.getBound());
        } else {
            menu.setDisplayPath("/" + menu.getMenuName());
        }
        if (parentMenu.getBound() != null && parentMenu.getBound() == true) {
            menu.setBoundMenuId(parentMenu.getBoundMenuId());
        }
        boolean save = menuService.save(menu);
        if (!save) {
            return Result.error("目录添加失败，请稍后再试");
        }
        menuUtil.onAddMenu(menu.getId(), menu.getParentId());
        return Result.success("目录添加成功");
    }

    @PostMapping("/updateMenu")
    public Result<String> updateMenu(@RequestBody Menu menu) throws RuntimeException {
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        if (!Objects.equals(userId, menu.getOwner())) {
            throw new BusinessException("权限不足");
        }
        Boolean nameDuplicate = menuService.checkNameDuplicate(menu.getParentId(), menu.getMenuName());
        if (nameDuplicate) {
            return Result.error("目录名称重复");
        }
        Menu oldMenu = menuService.getById(menu.getId());
        menu.setSource(1);
        menu.setDisplayPath(FileUtils.getNewDisplayPath(oldMenu.getDisplayPath(), menu.getMenuName()));
        boolean b = menuService.updateById(menu);
        menu = menuService.getById(menu.getId());
        if (!b) {
            return Result.error("修改失败");
        } else {
            conflictMap.addMenuConflict(menu, oldMenu.getMenuName(), 3);
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
        conflictMap.addMenuConflict(menu, null, 2);
        return Result.success("删除成功");
    }

    @PostMapping("/batchAddMenu")
    public Result<MenuTreeDTO> batchAddMenu(@RequestBody MenuTreeDTO menuTreeDTO) {
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        MenuTreeDTO list = menuService.batchAddMenu(menuTreeDTO, userId);
        return Result.success(list);
    }


    /**
     * 获取指定目录的子目录
     * 若 menuId 为空，则获取指定用户的所有一级目录
     * @return
     */
    @PostMapping("/getSubMenuList")
    public Result<MenuVO> getSubMenuList(@RequestBody SubMenuListDTO menuDTO) throws Exception {
        Long currentUserId = SecurityContextUtil.getCurrentUser().getId();
        // 当前目录的信息
        // menuId为空，当前处于一级目录
        Menu menu = new Menu();
        if (Objects.isNull(menuDTO.getMenuId())) {
            menu.setMenuLevel(1);
            menu.setOwner(currentUserId);
        } else {
            // 首先验证当前目录是否为当前用户的
            menu = menuService.getByUserAndMenuId(currentUserId, menuDTO.getMenuId());
            if (Objects.isNull(menu)) {
                return Result.error("请不要操作别人的数据");
            }
        }
        // 参数预处理
        // 搜索的类型，文件/目录
        final Integer type = menuDTO.getType() == null ? 0 : menuDTO.getType();
        // 排序规则，创建时间/修改时间
        final Integer orderBy = menuDTO.getOrderBy() == null ? 1 : menuDTO.getOrderBy();
        // 搜索name
        final String searchName = StringUtils.trimToEmpty(menuDTO.getName());

        // 分页参数处理
        int pageSize = menuDTO.getPageSize();
        int pageNum = menuDTO.getPageNum();
        int start = pageNum * pageSize;
        Long menuTotal = 0L;
        Long fileTotal = 0L;
        // 0或1时统计目录
        if (type != 2) {
            menuTotal = menuService.countByMenuId(menuDTO.getMenuId(), currentUserId, searchName);
        }
        // 0或2时统计文件
        if (type != 1) {
            fileTotal = fileService.countByMenuId(menuDTO.getMenuId(), currentUserId, searchName);
        }
        List<Menu> menuList = Collections.emptyList();
        List<File> fileList = Collections.emptyList();

        // 场景处理
        switch (type) {
            // 只看目录
            case 1:
                menuList = getMenuData(menuDTO.getMenuId(), currentUserId, start, pageSize, searchName, orderBy, menuDTO.getDesc());
                break;
            // 只看文件
            case 2:
                fileList = getFileData(menuDTO.getMenuId(), currentUserId, start, pageSize, searchName, orderBy, menuDTO.getDesc());
                break;
            // 全部
            default:
                FileAndMenuListVO vo = handleMixedData(menuDTO, currentUserId, start, pageSize, menuTotal, fileTotal, searchName, orderBy, menuDTO.getDesc());
                if (!CollectionUtils.isEmpty(vo.getFileList())) {
                    fileList = vo.getFileList();
                }
                if (!CollectionUtils.isEmpty(vo.getMenuList())) {
                    menuList = vo.getMenuList();
                }
        }

        List<MenuDetailVO> menuDetailVOList = new ArrayList<>();
        for (Menu m : menuList) {
            MenuDetailVO menuDetailVO = new MenuDetailVO();
            BeanUtils.copyProperties(m, menuDetailVO);
            menuDetailVOList.add(menuDetailVO);
        }
        // 填充目录大小
        if (!CollectionUtils.isEmpty(menuList)) {
            Map<Long, Long> idTOSizeMap = menuUtil.batchGetMenuSizes(menuList.stream().map(Menu::getId).collect(Collectors.toList()));
            menuDetailVOList.forEach(m -> {
                Long size = idTOSizeMap.get(m.getId());
                m.setMenuSize(size);
            });
        }

        MenuVO menuVO = new MenuVO()
                .setCurrentMenu(menu)
                .setSubMenuList(menuDetailVOList)
                .setSubFileList(fileList)
                .setPageNum(pageNum + 1)
                .setPageSize(pageSize)
                .setTotal((int) (menuTotal + fileTotal));
        return Result.success(menuVO);
    }
    // 分场景数据处理方法
    private FileAndMenuListVO handleMixedData(SubMenuListDTO dto,
                                 Long userId,
                                 int start,
                                 int pageSize,
                                 long menuTotal,
                                 long fileTotal,
                                 String name,
                                 int orderBy, Integer desc) {
        FileAndMenuListVO vo = new FileAndMenuListVO();
        if (start < menuTotal) {
            int dirCount = (int) Math.min(menuTotal - start, pageSize);
            List<Menu> menuList = menuService.getSubMenuByRange(dto.getMenuId(), userId, name, orderBy, desc, start, dirCount);
            vo.setMenuList(menuList);
            if (menuList.size() < pageSize) {
                int fileCount = pageSize - menuList.size();
                List<File> fileList = fileService.getSubFileByRange(dto.getMenuId(), userId, name, orderBy, desc, 0, fileCount);
                vo.setFileList(fileList);
            }
        } else {
            int fileStart = (int) (start - menuTotal);
            int adjustedSize = Math.min(pageSize, (int) (fileTotal - fileStart));
            List<File> fileList = fileService.getSubFileByRange(dto.getMenuId(), userId, name, orderBy, desc, fileStart, adjustedSize);
            vo.setFileList(fileList);
        }
        return vo;
    }

    /**
     * 获取目录数据
     */
    private List<Menu> getMenuData(Long menuId, Long userId, int start, int count, String name, Integer orderBy, Integer desc) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        return menuService.getSubMenuByRange(menuId, userId, name, orderBy, desc, start, count);
    }

    /**
     * 获取文件数据
     */
    private List<File> getFileData(Long menuId, Long userId, int start, int count, String name, Integer orderBy, Integer desc) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        return fileService.getSubFileByRange(menuId, userId, name, orderBy, desc, start, count);
    }

}
