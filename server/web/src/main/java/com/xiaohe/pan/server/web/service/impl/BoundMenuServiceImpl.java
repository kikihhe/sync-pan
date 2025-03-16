package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.server.web.mapper.BoundMenuMapper;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.service.BoundMenuService;
import org.springframework.stereotype.Service;

@Service
public class BoundMenuServiceImpl extends ServiceImpl<BoundMenuMapper, BoundMenu> implements BoundMenuService {
}
