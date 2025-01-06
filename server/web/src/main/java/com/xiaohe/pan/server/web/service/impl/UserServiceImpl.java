package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.server.web.mapper.UserMapper;
import com.xiaohe.pan.server.web.model.domain.User;
import com.xiaohe.pan.server.web.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User auth(String token) {
        return null;
    }

    @Override
    public User login(String username, String password) {
        LambdaQueryWrapper<User> lambda = new LambdaQueryWrapper<>();
        lambda.eq(User::getUsername, username);
        User user = baseMapper.selectOne(lambda);

        return null;
    }
}
