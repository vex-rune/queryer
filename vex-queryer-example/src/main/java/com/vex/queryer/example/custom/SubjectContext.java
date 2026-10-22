package com.vex.queryer.example.custom;

/**
 * 当前请求主体上下文 — 模拟 Web 层每请求设置、服务层读取的 ThreadLocal 缓存。
 */
public final class SubjectContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private SubjectContext() {
    }

    public static void set(String subjectId) {
        HOLDER.set(subjectId);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
