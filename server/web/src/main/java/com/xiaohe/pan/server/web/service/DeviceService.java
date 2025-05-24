package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.vo.DeviceHeartbeatVO;

import java.util.List;

public interface DeviceService extends IService<Device> {

    public Device verifyDeviceOwnership(String deviceKey, Long userId) throws BusinessException;

    Result<DeviceHeartbeatVO> processHeartbeat(Device device, List<String> boundedRemotePathList) throws BusinessException;

    Device verifySecret(String deviceKey, String secret);
}
