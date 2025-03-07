package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.server.web.mapper.UserMapper;
import com.xiaohe.pan.server.web.model.domain.User;
import com.xiaohe.pan.server.web.service.UserService;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsernameAndPassword(String username, String password) throws BusinessException {
        LambdaQueryWrapper<User> lambda = new LambdaQueryWrapper<>();
        lambda.eq(User::getUsername, username);
        User user = baseMapper.selectOne(lambda);
        if (!Objects.equals(user.getPassword(), password)) {
            return null;
        }
        return user;
    }
}
