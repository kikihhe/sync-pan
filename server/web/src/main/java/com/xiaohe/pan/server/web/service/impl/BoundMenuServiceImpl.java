package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.server.web.convert.BoundMenuConvert;
import com.xiaohe.pan.server.web.core.queue.BindingEventQueue;
import com.xiaohe.pan.server.web.mapper.BoundMenuMapper;
import com.xiaohe.pan.server.web.mapper.DeviceMapper;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;
import com.xiaohe.pan.server.web.service.BoundMenuService;
import com.xiaohe.pan.server.web.service.MenuService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
public class BoundMenuServiceImpl extends ServiceImpl<BoundMenuMapper, BoundMenu> implements BoundMenuService {

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private BindingEventQueue bindingEventQueue;

    @Resource
    private MenuService menuService;

    @Override
    public BoundMenu createBinding(Long userId, BoundMenu request) throws JsonProcessingException {
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
        if (Objects.isNull(m.getId()) && StringUtils.hasText(m.getDisplayPath())) {
            m.setDisplayPath(request.getRemoteMenuPath());
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
        baseMapper.insert(boundMenu);

        // 加入事件队列
        bindingEventQueue.addEvent(device.getDeviceKey(), boundMenu);
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
        return BoundMenuConvert.INSTANCE.boundMenuListConvertTOBoundMenuVOList(boundMenus);
    }
}
