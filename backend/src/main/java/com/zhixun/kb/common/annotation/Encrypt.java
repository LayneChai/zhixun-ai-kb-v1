package com.zhixun.kb.common.annotation;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypt {
    /**
     * 是否对入参解密
     */
    boolean in() default true;

    /**
     * 是否对出参加密
     */
    boolean out() default true;
}
