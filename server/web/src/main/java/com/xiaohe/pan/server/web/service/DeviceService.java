package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.vo.DeviceHeartbeatVO;

public interface DeviceService extends IService<Device> {

    public Device verifyDeviceOwnership(String deviceKey, String secret, Long userId) throws BusinessException;

    Result<DeviceHeartbeatVO> processHeartbeat(Device device) throws BusinessException;

    void verifySecret(String deviceKey, String secret);
}
