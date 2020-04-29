package com.xiaodayong.framework.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface myservice {
    String value() default "";
}
