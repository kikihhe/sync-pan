package com.xiaohe.pan.server.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaohe.pan.server.web.model.domain.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    @Update("UPDATE device SET status = 0 WHERE last_heartbeat < #{threshold} AND status != 0 and status = 1")
    int markDeadDevices(@Param("threshold") LocalDateTime threshold);
}
