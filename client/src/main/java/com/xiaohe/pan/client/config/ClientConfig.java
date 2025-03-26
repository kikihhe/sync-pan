package com.xiaohe.pan.client.config;

public class ClientConfig {
    private static String deviceKey;
    private static String secret;

    public static void init(String deviceKey, String secret) {
        ClientConfig.deviceKey = deviceKey;
        ClientConfig.secret = secret;
    }

    public static String getDeviceKey() { return deviceKey; }
    public static String getSecret() { return secret; }
}