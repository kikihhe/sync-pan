package com.xiaohe.pan.server.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;
import com.xiaohe.pan.common.model.vo.EventVO;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;
import com.xiaohe.pan.server.web.model.vo.ConflictVO;
import com.xiaohe.pan.server.web.service.BoundMenuService;
import com.xiaohe.pan.server.web.service.DeviceService;
import com.xiaohe.pan.server.web.service.MenuService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

import javax.annotation.Resource;


@RestController
@RequestMapping("/bound")
public class BoundMenuController {

    private static final Logger logger = LoggerFactory.getLogger(BoundMenuController.class);

    @Resource
    private BoundMenuService boundMenuService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private MenuService menuService;


    @PostMapping("/createBinding")
    public Result<BoundMenu> createBinding(@RequestBody BoundMenu request) throws JsonProcessingException {
        Long userId = SecurityContextUtil.getCurrentUserId();
        return Result.success(boundMenuService.createBinding(userId, request));
    }

    /**
     * 不再同步
     * @param bindingId
     * @return
     */
    @DeleteMapping("/{bindingId}")
    public Result<String> removeBinding(@PathVariable Long bindingId) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        boundMenuService.removeBinding(userId, bindingId);
        return Result.success("删除成功");
    }

    @PostMapping("/stopBinding/{bindingId}")
    public Result<String> stopBinding(@PathVariable Long bindingId) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        boundMenuService.stopBinding(userId, bindingId);
        return Result.success("删除成功");
    }

    @GetMapping("/{deviceId}")
    public Result<List<BoundMenuVO>> getDeviceBindings(@PathVariable Long deviceId) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        return Result.success(boundMenuService.getDeviceBindings(userId, deviceId));
    }

    @PostMapping("/sync")
    public Result<List<EventVO>> sync(@RequestBody EventsDTO eventsDTO) throws IOException {

        // 验证设备和密钥
        Device device = deviceService.verifySecret(
                String.valueOf(eventsDTO.getDeviceKey()),
                eventsDTO.getSecret()
        );

        List<EventVO> eventVOList = boundMenuService.sync(eventsDTO.getEvents());

        return Result.success("同步成功", eventVOList);
    }

    @PostMapping("/checkConflict")
    public Result<List<ConflictVO>> checkConflict(@RequestParam Long menuId) {
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        Menu menu = menuService.getById(menuId);
        if (Objects.isNull(menu)) {
            return Result.error("目录不存在");
        }
        if (!Objects.equals(userId, menu.getOwner())) {
            return Result.error("权限不足");
        }
        // 1. 先检查当前目录是否已经绑定
        if (!menu.getBound()) {
            return Result.error("目录未绑定");
        }
        List<ConflictVO> conflictList =  menuService.checkConflict(menu);
        return Result.success(conflictList);
    }

}
