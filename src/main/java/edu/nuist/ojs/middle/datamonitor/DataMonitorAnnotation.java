package edu.nuist.ojs.middle.datamonitor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface DataMonitorAnnotation {
    String configPoint();
}
