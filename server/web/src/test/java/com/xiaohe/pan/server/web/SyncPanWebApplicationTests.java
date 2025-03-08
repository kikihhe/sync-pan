package com.xiaohe.pan.server.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.server.web.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SyncPanWebApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testJWT() throws JsonProcessingException {
        User user = new User();
        user.setId(1L);
        user.setUsername("root");
        user.setPassword("123");
        user.setSalt("123123asdfasdf");
        // 生成 token
        String token = JWTUtils.createToken(user);
        System.out.println("生成的token ：" + token);

        // 解析
        User user1 = JWTUtils.parseAndConvert(token, User.class);
        System.out.println(user1);

    }
}
