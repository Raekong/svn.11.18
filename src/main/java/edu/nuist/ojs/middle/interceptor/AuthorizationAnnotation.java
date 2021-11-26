package edu.nuist.ojs.middle.interceptor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AuthorizationAnnotation {
    String configPoint();
    String role();
}
