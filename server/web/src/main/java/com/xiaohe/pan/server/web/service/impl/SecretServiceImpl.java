package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.server.web.mapper.SecretMapper;
import com.xiaohe.pan.server.web.model.domain.Secret;
import com.xiaohe.pan.server.web.service.SecretService;
import org.springframework.stereotype.Service;

@Service
public class SecretServiceImpl extends ServiceImpl<SecretMapper, Secret> implements SecretService {

}
