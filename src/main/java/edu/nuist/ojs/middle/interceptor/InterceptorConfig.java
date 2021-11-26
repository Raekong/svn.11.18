package edu.nuist.ojs.middle.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Autowired
    InterceptorRouter iRouter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //除了首页,测试页，静态前端代码和出错页，其它全拦截
        registry.addInterceptor(iRouter).addPathPatterns("/**")
                .excludePathPatterns( "/", "/img/**", "/js/**", "/css/**", "/errorPage" );

    }
}
