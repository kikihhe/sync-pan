package com.xiaohe.pan.server.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.convert.DeviceConvert;
import com.xiaohe.pan.server.web.enums.DeviceStatus;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.domain.Secret;
import com.xiaohe.pan.server.web.model.dto.DeviceHeartbeatDTO;
import com.xiaohe.pan.server.web.model.vo.DeviceHeartbeatVO;
import com.xiaohe.pan.server.web.model.vo.DeviceVO;
import com.xiaohe.pan.server.web.service.DeviceService;
import com.xiaohe.pan.server.web.service.SecretService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/device")
public class DeviceController {

    private static Logger logger = LoggerFactory.getLogger(DeviceController.class);

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SecretService secretService;

    @GetMapping("/getDeviceList")
    public Result<List<DeviceVO>> getDeviceList() {
        Long userId = SecurityContextUtil.getCurrentUserId();
        LambdaQueryWrapper<Device> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Device::getUserId, userId);
        List<Device> list = deviceService.list(lambda);
        List<Long> secretIdList = list.stream().map(Device::getSecretId).collect(Collectors.toList());
        List<Secret> secretList = secretService.listByIds(secretIdList);
        Map<Long, String> secretMap = secretList.stream().collect(Collectors.toMap(Secret::getId, Secret::getKey));
        List<DeviceVO> deviceVOList = list.stream().map(device -> {
            DeviceVO deviceVO = DeviceConvert.INSTANCE.deviceConvertTODeviceVO(device);
            String secretKey = secretMap.get(device.getSecretId());
            deviceVO.setSecretKey(secretKey);
            return deviceVO;
        }).collect(Collectors.toList());
        return Result.success(deviceVOList);
    }

    @PostMapping("/heartbeat")
    public Result<DeviceHeartbeatVO> heartbeat(HttpServletRequest request) throws BusinessException {
        String deviceKey = request.getHeader("deviceKey");
        String secret = request.getHeader("secret");
        if (!StringUtils.hasText(deviceKey) || !StringUtils.hasText(secret)) {
            return Result.error("设备未注册或密钥错误");
        }
        logger.info("收到来自 deviceKey=" + deviceKey + "的心跳请求");
        Device device = deviceService.verifySecret(deviceKey, secret);
        return deviceService.processHeartbeat(device);
    }

    @PostMapping("/registerDevice")
    public Result<Device> registerDevice(@RequestBody Device device) {
        Secret secret = secretService.getById(device.getSecretId());
        if (Objects.isNull(secret)) {
            return Result.error("密钥不存在");
        }
        Long userId = SecurityContextUtil.getCurrentUserId();
        device.setUserId(userId);
        if (!StringUtils.hasText(device.getDeviceKey())) {
            String deviceKey = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 16);
            device.setDeviceKey(deviceKey);
        }
        device.setStatus(DeviceStatus.REGISTER.getCode());
        deviceService.save(device);
        return Result.success(device);
    }
}
