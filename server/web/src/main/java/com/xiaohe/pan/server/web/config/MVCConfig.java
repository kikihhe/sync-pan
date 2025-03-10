package com.xiaohe.pan.server.web.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class MVCConfig implements WebMvcConfigurer {

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("/**")
//                .allowedMethods("GET", "POST", "PUT", "DELETE")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//    }
//    /**
//     * 配置解决跨域问题
//     * @param registry 跨域注册类
//     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        // 所有接口
//        registry.addMapping("/**")
//                // 是否发送 Cookie
//                .allowCredentials(true)
//                // 支持方法
//                .allowedMethods("*")
//                .allowedOriginPatterns("http://localhost:*")
//                .maxAge(3600)
//                .allowedHeaders("*");
//    }
}