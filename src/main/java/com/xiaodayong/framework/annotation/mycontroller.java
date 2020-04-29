package com.xiaodayong.framework.annotation;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface mycontroller {
    String value() default "";
}
