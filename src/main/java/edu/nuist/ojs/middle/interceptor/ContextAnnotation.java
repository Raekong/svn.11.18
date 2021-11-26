package edu.nuist.ojs.middle.interceptor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ContextAnnotation {
    String configPoint(); //当前拦截的CONTROLLER点
    String configKeys();   //需要获取的上下文对象或值,使用字符串定义，多个需要获取的，使用逗号分隔
}
