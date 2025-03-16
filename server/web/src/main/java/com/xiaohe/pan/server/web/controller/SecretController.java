package com.xiaohe.pan.server.web.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.Secret;
import com.xiaohe.pan.server.web.service.SecretService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/secret")
public class SecretController {

    @Resource
    private SecretService secretService;

    @PostMapping("/addSecret")
    public Result<String> addSecret(@RequestBody Secret secret) throws JsonProcessingException {
        if (StringUtils.isBlank(secret.getKey()) || StringUtils.isBlank(secret.getValue())) {
            return Result.error("参数错误");
        }
        Long userId = SecurityContextUtil.getCurrentUser().getId();
        secret.setUserId(userId);
        secretService.save(secret);
        String value = JWTUtils.createToken(secret);
        value = value.replace(".", "");
        secret.setValue(value);
        return Result.error("添加成功");
    }

    @GetMapping("/listSecret")
    public Result<List<Secret>> listSecret() {
        Long userId = SecurityContextUtil.getCurrentUserId();
        LambdaQueryWrapper<Secret> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Secret::getUserId, userId);
        List<Secret> list = secretService.list(lambda);
        return Result.success(list);
    }

    @PostMapping("/deleteSecret")
    public Result<String> deleteSecret(@RequestParam Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        LambdaQueryWrapper<Secret> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Secret::getUserId, userId);
        lambda.eq(Secret::getId, id);
        secretService.remove(lambda);
        return Result.success("删除成功");
    }
}
