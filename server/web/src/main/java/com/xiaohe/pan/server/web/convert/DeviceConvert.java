package com.xiaohe.pan.server.web.convert;

import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.vo.DeviceVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DeviceConvert {
    public static final DeviceConvert INSTANCE = Mappers.getMapper(DeviceConvert.class);

    DeviceVO deviceConvertTODeviceVO(Device device);
}
