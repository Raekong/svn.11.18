package edu.nuist.ojs.middle.datamonitor;

import com.alibaba.fastjson.JSONObject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import edu.nuist.ojs.middle.stub.CallStub;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;

@Component
@Aspect
public class DataMonitorAspect {
    @Autowired
    private CallStub callStub;

    @Before(value = "@annotation(DataMonitorAnnotation)")
    public void before(JoinPoint joinPoint){
      RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
      if(requestAttributes != null  ){
        HttpSession session = (HttpSession) requestAttributes.resolveReference(RequestAttributes.REFERENCE_SESSION);
        session.removeAttribute(MonitorDataAssembly.MONITOR_PARAMS);
      }
    }

    @After(value = "@annotation(DataMonitorAnnotation)")
    public void after(JoinPoint joinPoint){
      MethodSignature methodSignature=(MethodSignature)joinPoint.getSignature();
       Method method= methodSignature.getMethod();
       DataMonitorAnnotation dataMonitorAnnotation=method.getAnnotation(DataMonitorAnnotation.class);
       RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
       if(requestAttributes != null  ){
          HttpSession session = (HttpSession) requestAttributes.resolveReference(RequestAttributes.REFERENCE_SESSION);
          System.out.println("=====================----------------8888888888888888899999999999999999999");
          if( session.getAttribute(MonitorDataAssembly.MONITOR_PARAMS) != null ){
            String jsonString=JSONObject.toJSONString(session.getAttribute(MonitorDataAssembly.MONITOR_PARAMS));
            JSONObject data=JSONObject.parseObject(jsonString);
            callStub.callStub(dataMonitorAnnotation.configPoint(), String.class, "data", data);
            
          }
       }

   }
}
