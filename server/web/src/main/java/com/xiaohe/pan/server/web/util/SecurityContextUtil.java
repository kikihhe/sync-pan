package com.xiaohe.pan.server.web.util;

import com.xiaohe.pan.server.web.model.domain.User;

public class SecurityContextUtil {
    private static final ThreadLocal<User> THREAD_LOCAL = new ThreadLocal<>();
    public static void setCurrentUser(User user) {
        THREAD_LOCAL.set(user);
    }

    public static User getCurrentUser() {
        return THREAD_LOCAL.get();
    }

    public static Long getCurrentUserId() {
        return THREAD_LOCAL.get().getId();
    }
}
