package com.xiaohe.pan.server.web.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaohe.pan.server.web.model.domain.User;

public interface UserService extends IService<User> {

    public User login(String username, String password);

    public User auth(String token);

}
