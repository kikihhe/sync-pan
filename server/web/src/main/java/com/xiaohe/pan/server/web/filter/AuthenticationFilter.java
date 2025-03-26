package com.xiaohe.pan.server.web.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.JWTUtils;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.User;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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
import java.util.Arrays;
import java.util.List;

@WebFilter(filterName = "authenticationFilter")
//@Order(1)
public class AuthenticationFilter implements Filter {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static AntPathMatcher pathMatcher = new AntPathMatcher();
    /**
     * 通行白名单
     */
    private static List<String> allowList = Arrays.asList(
            "/hello",
            "/user/login",
            "/user/register",
            "/file/download",
            "/device/heartbeat"
//            "/device/"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException, BusinessException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 白名单检查
        if (allowAccess(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 添加OPTIONS请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("Authentication");
        // 接收来自不同源的请求，可以来自前端，也可以来自客户端
        if (StringUtils.hasText(token)) {
            User user = JWTUtils.parseAndConvert(token, User.class);
            SecurityContextUtil.setCurrentUser(user);
            filterChain.doFilter(request, response);
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            Result<Object> result = Result.error("身份验证失败!");
            String json = objectMapper.writeValueAsString(result);
            response.getWriter().write(json);
        }
    }

    private boolean allowAccess(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return allowList.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
