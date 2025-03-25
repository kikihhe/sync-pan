package com.xiaohe.pan.server.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.dto.DeviceHeartbeatDTO;
import com.xiaohe.pan.server.web.model.vo.DeviceHeartbeatVO;
import com.xiaohe.pan.server.web.service.DeviceService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/device")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;


    @GetMapping("/getDeviceList")
    public Result<List<Device>> getDeviceList() {
        Long userId = SecurityContextUtil.getCurrentUserId();
        LambdaQueryWrapper<Device> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Device::getUserId, userId);
        List<Device> list = deviceService.list(lambda);
        return Result.success(list);
    }

    @PostMapping("/heartbeat")
    public Result<DeviceHeartbeatVO> heartbeat(DeviceHeartbeatDTO heartbeatDTO) throws BusinessException {
        deviceService.verifySecret(heartbeatDTO.getDeviceKey(), heartbeatDTO.getSecret());
        Long userId = SecurityContextUtil.getCurrentUserId();
        Device device = deviceService.verifyDeviceOwnership(heartbeatDTO.getDeviceKey(), heartbeatDTO.getSecret(), userId);
        return deviceService.processHeartbeat(device);
    }



}
