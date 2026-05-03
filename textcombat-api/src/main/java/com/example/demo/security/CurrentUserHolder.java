package com.example.demo.security;

import com.example.demo.entity.UsersEntity;

public final class CurrentUserHolder {

    private static final ThreadLocal<UsersEntity> CURRENT = new ThreadLocal<>();

    private CurrentUserHolder() {}

    public static void set(UsersEntity user) {
        CURRENT.set(user);
    }

    public static UsersEntity get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
