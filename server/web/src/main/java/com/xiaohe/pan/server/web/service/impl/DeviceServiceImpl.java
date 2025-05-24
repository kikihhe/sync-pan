package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaohe.pan.common.enums.SyncCommandEnum;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.core.queue.BindingEventQueue;
import com.xiaohe.pan.server.web.enums.DeviceStatus;
import com.xiaohe.pan.server.web.mapper.DeviceMapper;
import com.xiaohe.pan.server.web.mapper.SecretMapper;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.domain.Secret;
import com.xiaohe.pan.server.web.model.event.BoundMenuEvent;
import com.xiaohe.pan.server.web.model.vo.DeviceHeartbeatVO;
import com.xiaohe.pan.server.web.service.BoundMenuService;
import com.xiaohe.pan.server.web.service.DeviceService;
import com.xiaohe.pan.server.web.util.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    @Resource
    private BindingEventQueue bindingEventQueue;

    @Resource
    private SecretMapper secretMapper;

    @Override
    public Device verifySecret(String deviceKey, String secretValue) {
        LambdaQueryWrapper<Device> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Device::getDeviceKey, deviceKey);
        Device device = baseMapper.selectOne(lambdaQueryWrapper);
        if (Objects.isNull(device)) {
            throw new BusinessException("设备不存在");
        }
        Secret secret = secretMapper.selectById(device.getSecretId());

        boolean verifySecret = CryptoUtils.verifySecret(secretValue, secret.getValue());
        if (!verifySecret) {
            throw new BusinessException("密钥错误!");
        }
        return device;
    }

    @Override
    public Device verifyDeviceOwnership(String deviceKey, Long userId) throws BusinessException {
        LambdaQueryWrapper<Device> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Device::getDeviceKey, deviceKey);
        Device device = baseMapper.selectOne(lambda);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (!device.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该设备");
        }
        return device;
    }
    @Autowired
    private BoundMenuService boundMenuService;

    @Override
    public Result<DeviceHeartbeatVO> processHeartbeat(Device device, List<String> boundedRemotePathList) throws BusinessException {
        // 更新心跳时间
        device.setLastHeartbeat(LocalDateTime.now());
        device.setStatus(DeviceStatus.HEALTH.getCode());
        baseMapper.updateById(device);

        // 查看是否有绑定事件
        log.info("心跳开始查看事件");
        List<BoundMenu> boundMenuList = boundMenuService.lambdaQuery().eq(BoundMenu::getDeviceId, device.getId()).list();
        // 本地未绑定的
        List<BoundMenu> unBoundedRemotePathList = boundMenuList.stream()
                .filter(i -> !boundedRemotePathList.contains(i.getRemoteMenuPath()))
                .collect(Collectors.toList());
        // 在这边已经删除的
        // 在boundedRemotePathList中存在，但是boundMenuList不存在的
        // 需要删除的（客户端有但服务端没有的）
        Map<String, BoundMenu> serverBoundMenuMap = boundMenuList.stream().collect(Collectors.toMap(BoundMenu::getRemoteMenuPath, m -> m));
        List<BoundMenu> removedBoundRemotePath = new ArrayList<>();
        for (String clientPath : boundedRemotePathList) {
            if (!serverBoundMenuMap.containsKey(clientPath)) {
                BoundMenu boundMenu = serverBoundMenuMap.get(clientPath);
                if (boundMenu == null) {
                    boundMenu = new BoundMenu();
                    boundMenu.setRemoteMenuPath(clientPath);
                }
                removedBoundRemotePath.add(boundMenu);
            }
        }
        log.info("心跳结束查看事件");

        // 组装
        List<BoundMenuEvent> boundMenuEventList = new ArrayList<>();
        // 绑定
        for (BoundMenu boundMenu : unBoundedRemotePathList) {
            BoundMenuEvent boundMenuEvent = new BoundMenuEvent();
            boundMenuEvent.setBoundMenu(boundMenu);
            boundMenuEvent.setType(1);
            boundMenuEventList.add(boundMenuEvent);
        }
        // 解绑
        for (BoundMenu boundMenu : removedBoundRemotePath) {
            BoundMenuEvent boundMenuEvent = new BoundMenuEvent();
            boundMenuEvent.setBoundMenu(boundMenu);
            boundMenuEvent.setType(2);
            boundMenuEventList.add(boundMenuEvent);
        }
        DeviceHeartbeatVO vo = new DeviceHeartbeatVO();
        vo.setPendingBindings(boundMenuEventList);
        vo.setSyncCommand(boundMenuEventList.isEmpty() ? SyncCommandEnum.WAIT.getCode() : SyncCommandEnum.SYNC_NOW.getCode());
        return Result.success(vo);
    }
}
