package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;

import java.util.List;

public interface BoundMenuService extends IService<BoundMenu> {
    BoundMenu createBinding(Long userId, BoundMenu request) throws JsonProcessingException;

    void removeBinding(Long userId, Long bindingId);

    List<BoundMenuVO> getDeviceBindings(Long userId, Long deviceId);
}
