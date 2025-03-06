package com.xiaohe.pan.server.web.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.User;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@WebFilter("authenticationFilter")
public class AuthenticationFilter implements Filter {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException, BusinessException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String token = request.getHeader("Authentication");

        // 接收来自不同源的请求，可以来自前端，也可以来自客户端
        if (StringUtils.hasText(token)) {
            User user = JWTUtils.parseAndConvert(token, User.class);
            SecurityContextUtil.setCurrentUser(user);
            doFilter(request, response, filterChain);
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            Result<Object> result = Result.error("身份验证失败!");
            String json = objectMapper.writeValueAsString(result);
            response.getWriter().write(json);
        }
    }
}
