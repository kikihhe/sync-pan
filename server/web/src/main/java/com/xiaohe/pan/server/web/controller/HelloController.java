package com.xiaohe.pan.server.web.controller;


import com.xiaohe.pan.common.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping
    public Result<String> hello() {
        return Result.success("你好");
    }

}
