package com.xiaohe.pan.server.web.service.impl;

import com.xiaohe.pan.server.web.mapper.DeviceMapper;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.service.DeviceService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {
}
