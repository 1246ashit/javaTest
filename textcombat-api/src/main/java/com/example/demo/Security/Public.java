package com.example.demo.Security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記在 Controller method 或 class 上，表示此 endpoint 不需登入即可存取。
 * 沒標這個注解的 endpoint 預設都要登入（token 有效）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Public {
}
