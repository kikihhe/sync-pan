package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;
import com.xiaohe.pan.common.model.vo.EventVO;
import com.xiaohe.pan.common.util.MD5Util;
import com.xiaohe.pan.server.web.convert.BoundMenuConvert;
import com.xiaohe.pan.server.web.core.queue.BindingEventQueue;
import com.xiaohe.pan.server.web.mapper.BoundMenuMapper;
import com.xiaohe.pan.server.web.mapper.DeviceMapper;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.UploadFileDTO;
import com.xiaohe.pan.server.web.model.event.BoundMenuEvent;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;
import com.xiaohe.pan.server.web.service.BoundMenuService;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.service.MenuService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BoundMenuServiceImpl extends ServiceImpl<BoundMenuMapper, BoundMenu> implements BoundMenuService {

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private BindingEventQueue bindingEventQueue;

    @Resource
    private MenuService menuService;

    @Resource
    private FileService fileService;

    @Override
    public BoundMenu createBinding(Long userId, BoundMenu request) throws RuntimeException {
        LambdaQueryWrapper<Device> deviceLambda = new LambdaQueryWrapper<>();
        deviceLambda.eq(Device::getUserId, userId);
        deviceLambda.eq(Device::getId, request.getDeviceId());
        Device device = deviceMapper.selectOne(deviceLambda);
        if (Objects.isNull(device)) {
            throw new BusinessException("设备不存在或无权操作");
        }

        // 查看远程目录是否已经绑定
        LambdaQueryWrapper<BoundMenu> lambda = new LambdaQueryWrapper<>();
        // 1. 选择了远程目录id
        if (!Objects.isNull(request.getRemoteMenuId())) {
            lambda.eq(BoundMenu::getRemoteMenuId, request.getRemoteMenuId());
        } else {
            // 2. 手动输入了一个全路径
            lambda.eq(BoundMenu::getRemoteMenuPath, request.getRemoteMenuPath());
        }
        BoundMenu boundMenu = baseMapper.selectOne(lambda);
        if (!Objects.isNull(boundMenu)) {
            throw new BusinessException("远程目录已绑定，请选择其他目录");
        }
        // 如果输入的路径，就去创建一个
        Menu m = new Menu();
        m.setId(request.getRemoteMenuId());
        m.setDisplayPath(request.getRemoteMenuPath());
        m.setOwner(userId);
        m.setSource(1);
        m.setBound(true);
        if (Objects.isNull(m.getId()) && StringUtils.hasText(m.getDisplayPath())) {
            m.setDisplayPath(request.getRemoteMenuPath());
            m.setBound(true);
            m = menuService.addMenuByPath(m);
        }
        // 创建绑定记录
        boundMenu = new BoundMenu()
                .setDeviceId(request.getDeviceId())
                .setLocalPath(request.getLocalPath())
                .setRemoteMenuId(m.getId())
                .setRemoteMenuPath(m.getDisplayPath())
                .setDirection(request.getDirection())
                .setStatus(1);
        int insert = baseMapper.insert(boundMenu);
        if (insert < 1) {
            log.error("绑定目录插入失败");
            throw new BusinessException("绑定失败!");
        }
        BoundMenuEvent event = new BoundMenuEvent();
        event.setBoundMenu(boundMenu);
        event.setType(1);
        // 加入事件队列
        bindingEventQueue.addEvent(device.getDeviceKey(), event);
        return boundMenu;
    }

    public void removeBinding(Long userId, Long bindingId) {
        BoundMenu boundMenu = baseMapper.selectById(bindingId);
        if (boundMenu == null || !deviceMapper.exists(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getId, boundMenu.getDeviceId())
                        .eq(Device::getUserId, userId))) {
            throw new BusinessException("绑定记录不存在或无权操作");
        }
        // 加入解绑事件队列
        BoundMenuEvent event = new BoundMenuEvent();
        event.setBoundMenu(boundMenu);
        event.setType(2);
        bindingEventQueue.addEvent(deviceMapper.selectById(boundMenu.getDeviceId()).getDeviceKey(), event);
        baseMapper.deleteById(bindingId);
    }

    @Override
    public List<BoundMenuVO> getDeviceBindings(Long userId, Long deviceId) {
        if (!deviceMapper.exists(new LambdaQueryWrapper<Device>()
                .eq(Device::getId, deviceId)
                .eq(Device::getUserId, userId))) {
            throw new BusinessException("设备不存在或无权访问");
        }

        LambdaQueryWrapper<BoundMenu> wrapper = new LambdaQueryWrapper<BoundMenu>()
                .eq(BoundMenu::getDeviceId, deviceId);
        List<BoundMenu> boundMenus = baseMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(boundMenus)) {
            return Collections.emptyList();
        }
        List<Long> remoteMenuIdList = boundMenus.stream().map(BoundMenu::getRemoteMenuId).collect(Collectors.toList());
        LambdaQueryWrapper<Menu> lambda = new LambdaQueryWrapper<>();
        lambda.select(Menu::getId, Menu::getDisplayPath);
        lambda.in(Menu::getId, remoteMenuIdList);
        Map<Long, String> id2DisplayPath = menuService.list(lambda).stream()
                .collect(Collectors.toMap(Menu::getId, Menu::getDisplayPath));
        List<BoundMenuVO> boundMenuVOList = BoundMenuConvert.INSTANCE.boundMenuListConvertTOBoundMenuVOList(boundMenus);
        for (BoundMenuVO menuVO : boundMenuVOList) {
            menuVO.setRemoteMenuPath(id2DisplayPath.get(menuVO.getRemoteMenuId()));
        }
        return boundMenuVOList;
    }

    private List<EventVO> sync(Long remoteMenuId, List<EventDTO> eventDTOList) throws IOException {
        // 处理每个事件
        List<EventDTO> menuEvents = new ArrayList<>();
        List<EventDTO> fileEvents = new ArrayList<>();
        // 获取绑定的顶级目录
        Menu boundMenu = menuService.getById(remoteMenuId);
        if (Objects.isNull(boundMenu)) {
            throw new BusinessException("目录不存在");
        }
        // 获取绑定记录
        LambdaQueryWrapper<BoundMenu> lambda = new LambdaQueryWrapper<>();
        lambda.eq(BoundMenu::getRemoteMenuId, boundMenu.getId());
        BoundMenu boundRecord = baseMapper.selectOne(lambda);
        if (Objects.isNull(boundRecord)) {
            throw new BusinessException("绑定记录不存在");
        }
        if (boundRecord.getStatus() == 2) {
            throw new BusinessException(boundRecord.getLocalPath() + " -> " + boundRecord.getRemoteMenuPath() + " 的同步已暂停");
        }
        // 将事件分类为目录事件和文件事件
        for (EventDTO eventDTO : eventDTOList) {
            switch (eventDTO.getType()) {
                case DIRECTORY_CREATE:
                case DIRECTORY_MODIFY:
                case DIRECTORY_DELETE:
                    menuEvents.add(eventDTO);
                    break;
                case FILE_CREATE:
                case FILE_MODIFY:
                case FILE_DELETE:
                    fileEvents.add(eventDTO);
                    break;
            }
        }
        menuEvents.sort((o1, o2) -> (int) (o1.getTimestamp() - o2.getTimestamp()));
        fileEvents.sort((o1, o2) -> (int) (o1.getTimestamp() - o2.getTimestamp()));
        // 修改最后同步时间
        boundRecord.setLastSyncedAt(LocalDateTime.now());
        baseMapper.updateById(boundRecord);
        // 返回结果
        return handleEvents(menuEvents, fileEvents, boundRecord, boundMenu);
    }

    @Override
    public List<EventVO> sync(List<EventDTO> allEventDTOList) throws IOException {
        Map<Long, List<EventDTO>> allBoundMenu = allEventDTOList.stream().collect(Collectors.groupingBy(EventDTO::getRemoteMenuId));
        List<EventVO> eventVOList = new ArrayList<>();
        allBoundMenu.forEach((remoteMenuId, eventDTOList) -> {
            try {
                List<EventVO> subEventVOList = sync(remoteMenuId, eventDTOList);
                eventVOList.addAll(subEventVOList);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
        return eventVOList;
    }

    @Override
    public void stopBinding(Long userId, Long boundMenuId) {
        BoundMenu boundMenu = getById(boundMenuId);
        if (Objects.isNull(boundMenu)) {
            throw new BusinessException("绑定目录不存在");
        }
        Device device = deviceMapper.selectById(boundMenu.getDeviceId());
        if (Objects.isNull(device)) {
            throw new BusinessException("设备不存在");
        }
        if (!device.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        boundMenu.setStatus(2);
        updateById(boundMenu);
    }

    public List<EventVO> handleEvents(List<EventDTO> menuEvents, List<EventDTO> fileEvents, BoundMenu boundRecord, Menu boundMenu) throws IOException {
        List<EventVO> eventVOList = new ArrayList<>();
        EventVO eventVO;
        // 处理目录事件
        for (EventDTO eventDTO : menuEvents) {
            switch (eventDTO.getType()) {
                case DIRECTORY_CREATE:
                    // 创建目录
                    eventVO = menuCreateEvent(boundRecord, boundMenu, eventDTO);
                    eventVOList.add(eventVO);
                    break;
                case DIRECTORY_MODIFY:
                    // 修改目录
                    break;
                case DIRECTORY_DELETE:
                    // 删除目录
                    eventVO = menuDeleteEvent(boundRecord, boundMenu, eventDTO);
                    eventVOList.add(eventVO);
                    break;
            }
        }

        // 处理文件事件
        for (EventDTO eventDTO : fileEvents) {
            switch (eventDTO.getType()) {
                case FILE_CREATE:
                    // 创建文件
                    eventVO = fileCreateEvent(boundRecord, boundMenu, eventDTO);
                    eventVOList.add(eventVO);
                    break;
                case FILE_MODIFY:
                    // 修改文件

                    break;
                case FILE_DELETE:
                    // 删除文件
                    eventVO = fileDeleteEvent(boundRecord, boundMenu, eventDTO);
                    eventVOList.add(eventVO);
                    break;
            }
        }
        return eventVOList;
    }

    private String calculateFileMD5(byte[] fileData) {
        return MD5Util.calculateMD5FromBytes(fileData);
    }

    /**
     * 计算本地路径对应的云端路径
     *
     * @param localBoundMenuPath 本地绑定的顶级目录，例如 D:/test/test1/test2
     * @param remoteMenuPath     远端绑定的顶级目录，例如 mac\a\b
     * @param localPath          本地路径，例如 D:/test/test1/test2/testK/testk
     * @return 需要在云端创建的路径，例如 mac\a\b\testK\testk
     */
    public String calculateRemotePath(String localBoundMenuPath, String remoteMenuPath, String localPath) {
        // 标准化路径分隔符，确保在不同操作系统下都能正确处理
        String normalizedLocalBoundPath = normalizePathSeparator(localBoundMenuPath);
        String normalizedLocalPath = normalizePathSeparator(localPath);

        // 确保本地路径是以本地绑定顶级目录开头的
        if (!normalizedLocalPath.startsWith(normalizedLocalBoundPath)) {
            throw new IllegalArgumentException("本地路径必须以本地绑定顶级目录开头");
        }

        // 计算相对路径（从本地绑定顶级目录到本地路径）
        String relativePath = "";
        if (normalizedLocalPath.length() > normalizedLocalBoundPath.length()) {
            // +1 是为了跳过路径分隔符
            relativePath = normalizedLocalPath.substring(normalizedLocalBoundPath.length() + 1);
        }

        // 构建云端路径
        if (relativePath.isEmpty()) {
            // 如果相对路径为空，则直接返回远端绑定顶级目录
            return remoteMenuPath;
        } else {
            // 否则，将相对路径附加到远端绑定顶级目录后面
            return remoteMenuPath + "/" + relativePath;
        }
    }

    /**
     * 标准化路径分隔符，将所有斜杠转换为统一格式
     */
    private String normalizePathSeparator(String path) {
        // 将所有反斜杠替换为正斜杠，然后再处理
        return path.replace('\\', '/');
    }


    private EventVO menuCreateEvent(BoundMenu boundRecord, Menu boundMenu, EventDTO eventDTO) {
        // 与云端绑定的本地的顶级目录
        String localBoundMenuPath = boundRecord.getLocalPath();
        // 与本地绑定的云端的顶级目录
        String remoteMenuPath = boundRecord.getRemoteMenuPath();
        // 计算本地的目录映射的云端路径
        String calculatedRemotePath = calculateRemotePath(localBoundMenuPath, remoteMenuPath, eventDTO.getLocalPath());
        String menuName = calculatedRemotePath.substring(calculatedRemotePath.lastIndexOf("/") + 1);
        Menu newMenu = new Menu();
        newMenu.setDisplayPath(calculatedRemotePath);
        newMenu.setOwner(boundMenu.getOwner());
        newMenu.setMenuName(menuName);
        newMenu.setSource(2);
        newMenu.setBound(true);
        menuService.addMenuByPath(newMenu);
        return buildEventVO(eventDTO, calculatedRemotePath);
    }

    private EventVO menuDeleteEvent(BoundMenu boundRecord, Menu boundMenu, EventDTO eventDTO) {
        // 与云端绑定的本地的顶级目录
        String localBoundMenuPath = boundRecord.getLocalPath();
        // 与本地绑定的云端的顶级目录
        String remoteMenuPath = boundRecord.getRemoteMenuPath();
        // 计算本地的目录映射的云端路径
        String calculatedRemotePath = calculateRemotePath(localBoundMenuPath, remoteMenuPath, eventDTO.getLocalPath());
        menuService.deleteMenuByPath(calculatedRemotePath);

        return buildEventVO(eventDTO, calculatedRemotePath);
    }

    private EventVO fileCreateEvent(BoundMenu boundRecord, Menu boundMenu, EventDTO eventDTO) throws IOException {
        // 与云端绑定的本地的顶级目录
        String localBoundMenuPath = boundRecord.getLocalPath();
        String remoteMenuPath = boundRecord.getRemoteMenuPath();
        String calculatedRemotePath = calculateRemotePath(localBoundMenuPath, remoteMenuPath, eventDTO.getLocalPath());
        String fileName = calculatedRemotePath.substring(calculatedRemotePath.lastIndexOf("/") + 1);
        String menuPath = calculatedRemotePath.substring(0, calculatedRemotePath.lastIndexOf("/"));
        Menu menu = menuService.getByDisplayPath(menuPath);
        if (Objects.isNull(menu)) {
            menu = new Menu();
            menu.setOwner(boundMenu.getOwner());
            menu.setDisplayPath(menuPath);
            menu.setSource(2);
            menu.setBound(true);
            menuService.addMenuByPath(menu);
        }
        File file = fileService.getFileByMenuIdAndFilename(menu.getId(), fileName);
        if (!Objects.isNull(file)) {
            fileService.deleteFile(Collections.singletonList(file.getId()));
        }
        UploadFileDTO uploadFileDTO = new UploadFileDTO();
        uploadFileDTO.setFileName(fileName);
        uploadFileDTO.setMenuId(menu.getId());
        uploadFileDTO.setFileSize((long) (eventDTO.getData().length));
        uploadFileDTO.setFileType(fileName.substring(fileName.lastIndexOf(".") + 1));
        uploadFileDTO.setIdentifier(calculateFileMD5(eventDTO.getData()));
        uploadFileDTO.setSource(2); // 本地同步过来的
        ByteInputStream bis = new ByteInputStream(eventDTO.getData(), eventDTO.getData().length);
        fileService.uploadFile(bis, uploadFileDTO);
        return buildEventVO(eventDTO, calculatedRemotePath);
    }

    private EventVO fileDeleteEvent(BoundMenu boundRecord, Menu boundMenu, EventDTO eventDTO) throws IOException {
        // 与云端绑定的本地的顶级目录
        String localBoundMenuPath = boundRecord.getLocalPath();
        String remoteMenuPath = boundRecord.getRemoteMenuPath();
        String calculatedRemotePath = calculateRemotePath(localBoundMenuPath, remoteMenuPath, eventDTO.getLocalPath());
        fileService.deleteByDisplayPath(calculatedRemotePath);
        return buildEventVO(eventDTO, calculatedRemotePath);
    }

    private EventVO buildEventVO(EventDTO eventDTO, String remotePath) {
        EventVO eventVO = new EventVO();
        eventVO.setRemotePath(remotePath);
        eventVO.setLocalPath(eventDTO.getLocalPath());
        eventVO.setEventType(eventDTO.getType());
        return eventVO;
    }

}
