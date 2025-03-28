package com.xiaohe.pan.server.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.domain.User;
import com.xiaohe.pan.server.web.service.MenuService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

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

    @Resource
    private MenuService menuService;

    @BeforeAll
    static void add() {
        User u = new User();
        u.setId(1L);
        u.setUsername("root");
        u.setPassword("123");
        SecurityContextUtil.setCurrentUser(u);
    }
    @Test
    void testAddMenuByPath() throws JsonProcessingException {
        Menu menu = new Menu();
        menu.setDisplayPath("/a/b/c/d");
        Menu menuByPath = menuService.addMenuByPath(menu);
        System.out.println(menuByPath);

    }


}
