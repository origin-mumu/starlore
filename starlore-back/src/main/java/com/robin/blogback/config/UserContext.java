package com.robin.blogback.config;

/** Request user identity propagated explicitly to Agent worker threads. */
public final class UserContext {

    private static final ThreadLocal<Integer> USER_ID = new ThreadLocal<>();

    private UserContext() {}

    public static void setUserId(Integer userId) {
        USER_ID.set(userId);
    }

    public static Integer getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
