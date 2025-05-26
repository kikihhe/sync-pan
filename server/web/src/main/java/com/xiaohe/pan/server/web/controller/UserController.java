package com.xiaohe.pan.server.web.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.User;
import com.xiaohe.pan.server.web.model.dto.LoginDTO;
import com.xiaohe.pan.server.web.service.FileService;
import com.xiaohe.pan.server.web.service.UserService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import com.xiaohe.pan.storage.api.StorageService;
import com.xiaohe.pan.storage.api.StoreTypeEnum;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Resource
    private StorageService storageService;

    @Value("${storage.type}")
    private String storageType;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${spring.mail.username}")
    private String from;

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO user) throws BusinessException, JsonProcessingException {
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

    @PostMapping("/sendVerifyCode")
    public Result sendVerifyCode(@RequestParam String email) {
        if (!StringUtils.hasText(email)) {
            return Result.error("邮箱不能为空");
        }

        // 生成6位验证码
        int i = new Random().nextInt(999999);
        String code = String.format("%06d", i);

        // 存储到Redis hash
        stringRedisTemplate.opsForHash().put("verify", email, code);
        stringRedisTemplate.expire("verify", 60 * 60, TimeUnit.SECONDS); // 60分钟有效期

        // 发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("sync-pan注册验证码");
        message.setText("您的注册验证码是：" + code + "，60分钟内有效");
        mailSender.send(message);

        return Result.success("验证码发送成功");
    }
    /**
     * 用户注册
     * @param user
     * @return
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user, @RequestParam String code) {
        // 验证码校验
        String storedCode = (String) stringRedisTemplate.opsForHash().get("verify", user.getEmail());
        if (!StringUtils.hasText(storedCode) || !storedCode.equals(code)) {
            return Result.error("验证码错误或已过期");
        }

        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("至少填写账号与密码!");
        }
        User exists = userService.lambdaQuery().eq(User::getUsername, user.getUsername()).one();
        if (exists != null) {
            return Result.error("用户名: "+ user.getUsername() + "已存在");
        }
        // 删除已使用的验证码， 先不删，测试用
//        stringRedisTemplate.opsForHash().delete("verify", user.getEmail());

        boolean insert = userService.save(user);
        if (!insert) {
            return Result.error("注册失败!请稍后再试");
        }
        return Result.success("注册成功", user);
    }

    @GetMapping("/currentUser")
    public Result<User> getCurrentUser() {
        Long userId = SecurityContextUtil.getCurrentUserId();
        User user = userService.lambdaQuery().eq(User::getId, userId).one();
        return Result.success(user);
    }

    @PostMapping("/uploadAvatar")
    public Result<User> uploadAvatar(@RequestBody MultipartFile file) throws IOException {
        Long userId = SecurityContextUtil.getCurrentUserId();
        User user = userService.lambdaQuery().eq(User::getId, userId).one();
        StoreFileContext storeFileContext = new StoreFileContext()
                .setFilename(file.getOriginalFilename())
                .setTotalSize(file.getSize())
                .setInputStream(file.getInputStream());
        // 调用文件服务，文件的真实路径会保存在 context 中
        storageService.store(storeFileContext);

        File rawFile = new File();
        rawFile.setFileType(Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf(".")));
        rawFile.setFileSize(file.getSize());
        rawFile.setRealPath(storeFileContext.getRealPath());
        rawFile.setSource(1);
        // 头像统统设置为 -1 用户的
        rawFile.setOwner(-1L);
        rawFile.setMenuId(-1L);
        rawFile.setSource(1);
        rawFile.setIdentifier("jeoqifnwjf");
        rawFile.setDisplayPath("----/");
        rawFile.setStorageType(StoreTypeEnum.getCodeByDesc(storageType));
        rawFile.setFileName(file.getOriginalFilename());
        fileService.save(rawFile);
        user.setAvatar(rawFile.getRealPath());
        userService.updateById(user);
        return Result.success(user);
    }
}
