package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.service.MenuService;
import org.springframework.stereotype.Service;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {
}
