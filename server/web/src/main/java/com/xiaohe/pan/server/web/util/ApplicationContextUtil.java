package com.xiaohe.pan.server.web.util;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextUtil implements ApplicationContextAware {
    public static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext app) throws BeansException {
        applicationContext = app;
    }

    public static <T> T getBeanByClass(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
