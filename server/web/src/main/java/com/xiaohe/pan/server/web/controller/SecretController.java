package com.xiaohe.pan.server.web.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.Secret;
import com.xiaohe.pan.server.web.service.SecretService;
import com.xiaohe.pan.server.web.util.CryptoUtils;
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
    public Result<String> addSecret(@RequestBody Secret request) throws JsonProcessingException {
        if (StringUtils.isBlank(request.getKey()) || StringUtils.isBlank(request.getValue())) {
            return Result.error("参数错误");
        }
        Long userId = SecurityContextUtil.getCurrentUserId();

        Secret secret = new Secret()
                .setUserId(userId)
                .setKey(request.getKey())
                .setValue(CryptoUtils.hashSecret(request.getValue()));

        secretService.save(secret);
        return Result.success();
    }

    @GetMapping("/listSecret")
    public Result<List<Secret>> listSecret(@RequestParam String key) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        LambdaQueryWrapper<Secret> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Secret::getUserId, userId);
        if (!StringUtils.isBlank(key)) {
            lambda.like(Secret::getKey, key);
        }
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
