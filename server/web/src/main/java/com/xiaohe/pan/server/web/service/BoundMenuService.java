package com.xiaohe.pan.server.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;
import com.xiaohe.pan.common.model.vo.EventVO;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.dto.ResolveConflictDTO;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;
import com.xiaohe.pan.server.web.model.vo.MenuConflictVO;
import com.xiaohe.pan.server.web.model.vo.ResolvedConflictVO;

import java.io.IOException;
import java.util.List;

public interface BoundMenuService extends IService<BoundMenu> {
    BoundMenu createBinding(Long userId, BoundMenu request) throws JsonProcessingException;

    void removeBinding(Long userId, Long bindingId);

    List<BoundMenuVO> getDeviceBindings(Long userId, Long deviceId);

    List<EventVO> sync(List<EventDTO> eventDTOList) throws IOException;

    void stopBinding(Long userId, Long bindingId);

    void resolveConflict(ResolveConflictDTO menuConflictVO) throws IOException;

    ResolvedConflictVO getResolvedConflict(Menu menu);

    BoundMenu getBoundMenuByMenuId(Long menuId);
}
