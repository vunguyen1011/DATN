package com.example.datn.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int requests() default 5;    // Số lượng request tối đa cho phép
    int window() default 30;     // Trong khoảng thời gian (tính bằng giây)
}