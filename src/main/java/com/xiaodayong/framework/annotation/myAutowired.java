package com.xiaodayong.framework.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface myAutowired {
    String value() default "";
}
