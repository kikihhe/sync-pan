package com.xiaohe.pan.server.web.controller;


import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.User;
import com.xiaohe.pan.server.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Objects;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody User user) throws BusinessException {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("请填完整信息!", user);
        }
        User rawUser = userService.getByUsernameAndPassword(user.getUsername(), user.getPassword());
        if (Objects.isNull(rawUser)) {
            return Result.error("账号/密码错误!", user);
        }
        rawUser.setPassword(null);
        String token = JWTUtils.createToken(rawUser);
        return Result.success("登陆成功", token);
    }

    /**
     * 登出，将 token 删除
     * @return
     */
    @RequestMapping("/logout")
    public Result logout() {
        return Result.success("退出登录");
    }

    /**
     * 用户注册
     * @param user
     * @return
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("至少填写账号与密码!", user);
        }
        boolean insert = userService.save(user);
        if (!insert) {
            return Result.error("注册失败!请稍后再试");
        }
        return Result.success("注册成功", user);
    }
}
