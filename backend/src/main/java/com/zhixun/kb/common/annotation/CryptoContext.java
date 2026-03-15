package com.zhixun.kb.common.annotation;

public class CryptoContext {
    private static final ThreadLocal<String> AES_KEY = new ThreadLocal<>();

    public static void setAesKey(String key) {
        AES_KEY.set(key);
    }

    public static String getAesKey() {
        return AES_KEY.get();
    }

    public static void clear() {
        AES_KEY.remove();
    }
}
