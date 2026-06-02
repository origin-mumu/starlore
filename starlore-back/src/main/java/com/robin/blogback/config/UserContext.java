package com.robin.blogback.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserContext {

    private static final ThreadLocal<Integer> USER_ID = new InheritableThreadLocal<>();

    /**
     * 跨线程用户映射，用于 AI Agent 工具调用场景。
     * key: 线程组标识（如 SSE 连接 ID），value: 用户 ID。
     */
    private static final Map<String, Integer> CROSS_THREAD_USER_MAP = new ConcurrentHashMap<>();

    public static void setUserId(Integer userId) {
        USER_ID.set(userId);
    }

    /**
     * 为跨线程场景注册用户 ID。
     */
    public static void setCrossThreadUser(String key, Integer userId) {
        CROSS_THREAD_USER_MAP.put(key, userId);
    }

    /**
     * 获取当前线程的用户 ID。
     * 优先级：ThreadLocal > 跨线程映射
     */
    public static Integer getUserId() {
        Integer uid = USER_ID.get();
        if (uid != null) return uid;
        // 回退：从跨线程映射中查找
        Integer crossUid = CROSS_THREAD_USER_MAP.get("agent-stream");
        if (crossUid != null) return crossUid;
        // 兜底：只有一个用户时直接返回
        if (CROSS_THREAD_USER_MAP.size() == 1) {
            return CROSS_THREAD_USER_MAP.values().iterator().next();
        }
        return null;
    }

    /**
     * 获取指定跨线程标识的用户 ID。
     */
    public static Integer getCrossThreadUser(String key) {
        return CROSS_THREAD_USER_MAP.get(key);
    }

    public static void clear() {
        USER_ID.remove();
    }

    /**
     * 清除指定跨线程标识的用户映射。
     */
    public static void clearCrossThread(String key) {
        CROSS_THREAD_USER_MAP.remove(key);
    }

    /**
     * 清除所有跨线程用户映射（谨慎使用）。
     */
    public static void clearAllCrossThread() {
        CROSS_THREAD_USER_MAP.clear();
    }
}
