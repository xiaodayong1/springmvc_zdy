package com.xiaodayong.framework.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface myRequestMapping {

    String value() default "";

}
