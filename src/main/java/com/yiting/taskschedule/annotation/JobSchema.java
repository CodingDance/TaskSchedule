package com.yiting.taskschedule.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by hzyiting on 2016/5/26.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JobSchema {

    int MODE_SYNC=0;
    int MODE_ASYNC=1;

    int mode() default MODE_SYNC;

    String desc() default "job";

}
