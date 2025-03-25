package com.xiaohe.pan.server.web.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CryptoUtils {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static String hashSecret(String rawSecret) {
        return encoder.encode(rawSecret);
    }

    public static boolean verifySecret(String rawSecret, String hashedSecret) {
        return encoder.matches(rawSecret, hashedSecret);
    }
}