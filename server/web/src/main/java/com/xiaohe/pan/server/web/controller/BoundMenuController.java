package com.xiaohe.pan.server.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;
import com.xiaohe.pan.common.model.dto.MergeEvent;
import com.xiaohe.pan.common.model.vo.EventVO;
import com.xiaohe.pan.common.util.FileUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.core.queue.ConflictMap;
import com.xiaohe.pan.server.web.core.queue.MergeEventQueue;
import com.xiaohe.pan.server.web.enums.BoundMenuDirection;
import com.xiaohe.pan.server.web.enums.DeviceStatus;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.ResolveConflictDTO;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;
import com.xiaohe.pan.server.web.model.vo.ConflictVO;
import com.xiaohe.pan.server.web.model.vo.ResolvedConflictVO;
import com.xiaohe.pan.server.web.service.BoundMenuService;
import com.xiaohe.pan.server.web.service.DeviceService;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.service.MenuService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import com.xiaohe.pan.storage.api.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


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

    @Resource
    private MergeEventQueue mergeEventQueue;
    @Autowired
    private ConflictMap conflictMap;
    @Resource
    private StorageService storageService;
    @Autowired
    private FileService fileService;


    @PostMapping("/createBinding")
    public Result<BoundMenu> createBinding(@RequestBody BoundMenu request) throws JsonProcessingException {
        if (request.getRemoteMenuId() == null && !StringUtils.hasText(request.getRemoteMenuPath())) {
            return Result.error("文件不存在，或者权限不足(禁止绑定顶级目录)");
        }
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
        // 检查设备状态
        if (device.getStatus() != 1) {
            return Result.error("设备状态异常: 设备" + DeviceStatus.getByCode(device.getStatus()).getDesc());
        }

        List<EventVO> eventVOList = boundMenuService.sync(eventsDTO.getEvents());

        return Result.success("同步成功", eventVOList);
    }

    /**
     * 在 checkConflict 时，这个放在中间，展示“已经被解决了”
     * @param menuId
     * @return
     * @throws IOException
     */
    @PostMapping("/getResolvedConflict")
    public Result<ResolvedConflictVO> getResolvedConflict(@RequestParam Long menuId) throws IOException {
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
        ResolvedConflictVO vo = boundMenuService.getResolvedConflict(menu);
        return Result.success(vo);
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
        Long boundMenuId = menu.getBoundMenuId();
        BoundMenu boundMenu = boundMenuService.getById(boundMenuId);
        if (Objects.isNull(boundMenu)) {
            return Result.error("绑定目录不存在");
        }
        if (Objects.equals(boundMenu.getDirection(), BoundMenuDirection.UP.getCode())) {
            return Result.error("同步目录为上传方式，禁止冲突检查");
        }
        List<ConflictVO> conflictList =  menuService.checkConflict(menu);
        // 冲突合并逻辑
        if (!conflictList.isEmpty()) {
            // 获取冲突map中的冲突
            ConflictVO mapConflicts = conflictMap.getAllConflicts(menu.getDisplayPath());

            // 处理本地冲突（index 1）
            ConflictVO localConflict = conflictList.get(1);
            filterConflicts(localConflict, mapConflicts);

            // 处理云端冲突（index 0）
            if (conflictList.size() > 1) {
                ConflictVO cloudConflict = conflictList.get(0);
                mergeConflicts(cloudConflict, mapConflicts);
            }
        }

        return Result.success(conflictList);
    }

    /**
     * 用户在网站解决冲突
     * @return
     */
    @PostMapping("/resolveConflict")
    public Result<String> resolveConflict(@RequestBody ResolveConflictDTO dto) throws IOException {
        boundMenuService.resolveConflict(dto);
        return Result.success("冲突解决成功");
    }

    /**
     * 用户解决冲突后，设备调用这个接口获取用户合并事件
     * @param request
     * @return
     * @throws InterruptedException
     */
    @PostMapping("/getMergedEvents")
    public Result<List<MergeEvent>> getMergedEvents(HttpServletRequest request) throws InterruptedException {
        String deviceKey = request.getHeader("deviceKey");
        String secret = request.getHeader("secret");
        if (!StringUtils.hasText(deviceKey) || !StringUtils.hasText(secret)) {
            return Result.result(505, "设备 " + deviceKey + " 未注册或密钥错误", null);
        }
//        logger.info("收到来自 deviceKey=" + deviceKey + "的心跳请求");
        Device device = deviceService.verifySecret(deviceKey, secret);

        List<ResolveConflictDTO> resolveConflictDTOS = mergeEventQueue.pollResolveConflict(deviceKey);
        if (CollectionUtils.isEmpty(resolveConflictDTOS)) {
            return Result.success();
        }
        // 这台设备上所有的绑定目录
        List<BoundMenu> allBoundMenuList = boundMenuService.lambdaQuery().eq(BoundMenu::getDeviceId, device.getId()).list();
        if (CollectionUtils.isEmpty(allBoundMenuList)) {
            return Result.success();
        }
        List<Long> allBoundMenuIDList = allBoundMenuList.stream().map(BoundMenu::getId).collect(Collectors.toList());

        List<MergeEvent> mergeEvents = new ArrayList<>();
        Map<Long, BoundMenu> idToBoundMenuMap = allBoundMenuList.stream().collect(Collectors.toMap(BoundMenu::getId, boundMenu -> boundMenu));
        resolveConflictDTOS.forEach(resolveConflictDTO -> {
            Menu currentMenu = resolveConflictDTO.getCurrentMenu();
            List<File> fileItems = resolveConflictDTO.getFileItems().stream().filter(i -> allBoundMenuIDList.contains(i.getBoundMenuId())).collect(Collectors.toList());
            List<Menu> menuItems = resolveConflictDTO.getMenuItems().stream().filter(i -> allBoundMenuIDList.contains(i.getBoundMenuId())).collect(Collectors.toList());
            fileItems.forEach(fileItem -> {
                BoundMenu boundMenu = idToBoundMenuMap.get(fileItem.getBoundMenuId());
                MergeEvent mergeEvent = new MergeEvent();
                mergeEvent.setLocalBoundMenuPath(boundMenu.getLocalPath());
                mergeEvent.setRemoteBoundMenuPath(boundMenu.getRemoteMenuPath());
                mergeEvent.setFilename(fileItem.getFileName());
                String remoteMenuPath = FileUtils.getNewDisplayPath(fileItem.getDisplayPath(), "");
                mergeEvent.setFileType(2);
                mergeEvent.setResolveRemotePath(currentMenu.getDisplayPath());
                mergeEvent.setRemoteMenuPath(remoteMenuPath);
                try {
                    byte[] bytes = fileService.readFile(fileItem);
                    mergeEvent.setData(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                mergeEvents.add(mergeEvent);
            });
            menuItems.forEach(menuItem -> {
                BoundMenu boundMenu = idToBoundMenuMap.get(menuItem.getBoundMenuId());
                MergeEvent mergeEvent = new MergeEvent();
                mergeEvent.setLocalBoundMenuPath(boundMenu.getLocalPath());
                mergeEvent.setRemoteBoundMenuPath(boundMenu.getRemoteMenuPath());
                mergeEvent.setFilename(menuItem.getMenuName());
                mergeEvent.setFileType(1);
                String remoteMenuPath = FileUtils.getNewDisplayPath(menuItem.getDisplayPath(), null);
                mergeEvent.setRemoteMenuPath(remoteMenuPath);
                mergeEvent.setResolveRemotePath(currentMenu.getDisplayPath());
                mergeEvents.add(mergeEvent);
            });
        });
        mergeEventQueue.clearResolveConflict(deviceKey, null);
        return Result.success(mergeEvents);
    }


    // 过滤本地冲突中已存在冲突条目
    private void filterConflicts(ConflictVO target, ConflictVO mapConflicts) {
        target.getFileConflictVOList().removeIf(f ->
                mapConflicts.getFileConflictVOList().stream()
                        .anyMatch(mf -> mf.getFile().getDisplayPath().equals(f.getFile().getDisplayPath()))
        );
        target.getMenuConflictVOList().removeIf(m ->
                mapConflicts.getMenuConflictVOList().stream()
                        .anyMatch(mm -> mm.getMenu().getDisplayPath().equals(m.getMenu().getDisplayPath()))
        );
    }

    // 合并冲突到云端冲突（以map数据为准）
    private void mergeConflicts(ConflictVO target, ConflictVO mapConflicts) {
        // 合并文件冲突
        mapConflicts.getFileConflictVOList().forEach(mf -> {
            target.getFileConflictVOList().removeIf(f ->
                    f.getFile().getDisplayPath().equals(mf.getFile().getDisplayPath()));
            target.getFileConflictVOList().add(mf);
        });

        // 合并目录冲突
        mapConflicts.getMenuConflictVOList().forEach(mm -> {
            target.getMenuConflictVOList().removeIf(m ->
                    m.getMenu().getDisplayPath().equals(mm.getMenu().getDisplayPath()));
            target.getMenuConflictVOList().add(mm);
        });
    }
}
