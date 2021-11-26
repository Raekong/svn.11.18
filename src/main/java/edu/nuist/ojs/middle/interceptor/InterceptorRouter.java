package edu.nuist.ojs.middle.interceptor;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import edu.nuist.ojs.middle.controller.PageComponentRouter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;


/**
 * @author Han jin
 * 定义了二个接截器。一个是用于权限检查，一个用于上下文检查和参数获取
 * 拦截器对应的标注，可以放在方法上，也可以放在类上，如果都有，方法会覆盖掉类的标注属性定义
 */
@Component
public class InterceptorRouter implements HandlerInterceptor {
    @Autowired
    private ContextInterceptor cInterceptor;

    @Autowired
    private AuthorizationInterceptor aInterceptor;

    @Autowired
    private I18NInterceptor i18nInterceptor;

    @Value("${interception.error.htmlurl}")
    private String errorUrl;
    
    @Value("${interception.error.jsonStr}")
    private String errorjson;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response, Object handler) throws IOException {
        return this.process(request, handler, response);
    }

    public boolean process(
        HttpServletRequest request,
        Object handler, 
        HttpServletResponse response) throws IOException {
          
        if (handler instanceof HandlerMethod) {
            JSONObject interceptorFlag = null;

            String type = "";
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            if ( handlerMethod.getMethod().getDeclaringClass().getAnnotation(Controller.class) != null){
                type = "html";
            }else if (handlerMethod.getMethod().getDeclaringClass().getAnnotation(RestController.class) != null){
                type = "rest";
            } 
            
            if("".equals(type)) return true;

            interceptorFlag = isMatch(response,  handlerMethod, request);
            if( interceptorFlag != null ) { //不等于null 就是失败
                if( "html".equals(type) ){
                    request.setAttribute("configPoint",  interceptorFlag.getString("configPoint"));
                    request.setAttribute("configKey",  interceptorFlag.getString("configKey"));
                    request.setAttribute("configType", interceptorFlag.getString("configType"));
                    htmlError( request, response );
                }else{
                    restError(response, MessageFormat.format(  errorjson, 
                                            interceptorFlag.getString("configType"),
                                            interceptorFlag.getString("configPoint"),
                                            interceptorFlag.getString("configKey"))
                    );
                }
                return false;
            }
        }
        return true;
    }

    public JSONObject isMatch(
        HttpServletResponse response,
        HandlerMethod handlerMethod,
        HttpServletRequest request ) throws IOException {
           
        JSONObject obj = null;
        //如果有权限先检查
        AuthorizationAnnotation authorizationAnnotation = handlerMethod.getMethod().getAnnotation(AuthorizationAnnotation.class);
        if( authorizationAnnotation == null ){
            //如果方法上没有，就去类上拿
            authorizationAnnotation = handlerMethod.getMethod().getDeclaringClass().getAnnotation(AuthorizationAnnotation.class);
        }
        if( authorizationAnnotation != null ){
            String role = authorizationAnnotation.role();
            boolean flag = aInterceptor.exec(request.getSession(),  role);
            if( !flag ){ //检测失败直接返回
                obj = new JSONObject();
                obj.put("flag" , false );
                obj.put("configType", "Authorization");
                obj.put("configPoint", authorizationAnnotation.configPoint());
                obj.put("configKey", authorizationAnnotation.role());
                return obj;
            } 
        }
        
        //如果没有权限再检查CONTEXT
        ContextAnnotation contextAnnotation = handlerMethod.getMethod().getAnnotation( ContextAnnotation.class );
        if( contextAnnotation == null ){
            //如果方法上没有，就去类上拿
            contextAnnotation = handlerMethod.getMethod().getDeclaringClass().getAnnotation(ContextAnnotation.class);
        }
        if( contextAnnotation != null ){
            String configKeys = contextAnnotation.configKeys();
            boolean flag = cInterceptor.exec(request.getSession(), configKeys);
            if( !flag ){ //检测失败直接返回
                obj = new JSONObject();
                obj.put("flag" , false );
                obj.put("configType", "Context");
                obj.put("configPoint", contextAnnotation.configPoint());
                obj.put("configKey", contextAnnotation.configKeys());
                return obj;
            } 
        }

        return null;
    }

    
    public void htmlError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            //请求转发到出错页面
            request.getRequestDispatcher( errorUrl ).forward(request, response);
        } catch (ServletException e) {
            e.printStackTrace();
        } 
    }

    public void restError(HttpServletResponse response, String errorStr) throws IOException {
        PrintWriter out = null;
        out = response.getWriter(); //直接输出出错信息
        out.append(errorStr);
    }

    @Override
    public void postHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler, 
        ModelAndView modelAndView) {
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                I18NAnnotation i18NAnnotation = handlerMethod.getMethod().getAnnotation( I18NAnnotation.class );
                if( i18NAnnotation != null){
                    String keys = i18NAnnotation.configKeys();
                    if( "".equals( keys) ){
                        keys = (String)request.getAttribute(PageComponentRouter.COMPONENTI18NKEY);
                    }
                    i18nInterceptor.exec(request, modelAndView, keys );
                }
            }
            
    }

    //清除context拦截器可能放入的参数
    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler, 
        Exception ex) {
    }

}
