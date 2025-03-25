package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaohe.pan.common.enums.SyncCommandEnum;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.core.queue.BindingEventQueue;
import com.xiaohe.pan.server.web.mapper.DeviceMapper;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.vo.DeviceHeartbeatVO;
import com.xiaohe.pan.server.web.service.DeviceService;
import com.xiaohe.pan.server.web.util.CryptoUtils;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    @Resource
    private BindingEventQueue bindingEventQueue;

    @Override
    public void verifySecret(String deviceKey, String secret) {
        LambdaQueryWrapper<Device> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.select(Device::getSecret);
        lambdaQueryWrapper.eq(Device::getDeviceKey, deviceKey);
        Device device = baseMapper.selectOne(lambdaQueryWrapper);
        if (Objects.isNull(device)) {
            throw new BusinessException("设备不存在");
        }

        boolean verifySecret = CryptoUtils.verifySecret(secret, device.getSecret());
        if (!verifySecret) {
            throw new BusinessException("密钥错误!");
        }
    }

    @Override
    public Device verifyDeviceOwnership(String deviceKey, String secret, Long userId) throws BusinessException {
        LambdaQueryWrapper<Device> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Device::getDeviceKey, deviceKey);
        Device device = baseMapper.selectOne(lambda);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (!device.getSecret().equals(secret)) {
            throw new BusinessException("同步密钥错误");
        }
        if (!device.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该设备");
        }
        return device;
    }

    @Override
    public Result<DeviceHeartbeatVO> processHeartbeat(Device device) throws BusinessException {
        // 更新心跳时间
        device.setLastHeartbeat(LocalDateTime.now());
        baseMapper.updateById(device);

        // 查看是否有绑定事件
        List<BoundMenu> boundMenus = bindingEventQueue.pollEvents(device.getDeviceKey());

        DeviceHeartbeatVO vo = new DeviceHeartbeatVO();
        vo.setPendingBindings(boundMenus);
        vo.setSyncCommand(boundMenus.isEmpty() ? SyncCommandEnum.WAIT.getCode() : SyncCommandEnum.SYNC_NOW.getCode());
        return Result.success(vo);
    }
}
