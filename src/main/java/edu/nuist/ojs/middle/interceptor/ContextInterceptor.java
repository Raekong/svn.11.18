package edu.nuist.ojs.middle.interceptor;

import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.journalsetting.JournalRole;
import edu.nuist.ojs.middle.context.Context;

import org.assertj.core.util.Arrays;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * 检查用户CONTEXT操作
 *      在ContextAnnotation中有字段定义所需要的上下文参数，在完成操作之后，
 *      将会从上下文中取出参数，放置到用户的Session，由Controller方法去直接获取
 * 
 * 检查的方法是：
 *      1. 首先判断用户的Session中有没有CONTEXT对象，没有就返回FALSE
 *      2. 再直接调用CONTEXT对象的方法，去拿KEY的值，将KEY按逗号分隔，逐个取，并入Session中
 *      3. CONFIGKEY的组织方式 key1:type, key2:type， 这样的方式来定义要取的值及类型
 */
@Component
public class ContextInterceptor {
    public static final String CONTEXT = "Context";

    //获得在上下文拦截器中拦截CONTEXT对象中存储的数据
    public Object get(String key, HttpSession session){
        return ((Context)session.getAttribute(CONTEXT)).get(key);
    }

    public void setPublisher(Publisher p, HttpSession session){
        Context c = (Context)( session.getAttribute(CONTEXT));
        c.setPublisher(p);
        return;
    }

    public void setUser(User u, HttpSession session){
        Context c = (Context)( session.getAttribute(CONTEXT));
        c.setUser(u);
        return;
    }

    public void setSomething(String key, Object v, HttpSession session){
        Context c = (Context)( session.getAttribute(CONTEXT));
        c.getSomething().put(key, v);
        return;
    }

    public void setRoles(HashMap<Long, JournalRole[]> roles, HttpSession session){
        Context c = (Context)( session.getAttribute(CONTEXT));
        c.setRoles(roles);
        return;
    }

    //初始化CONTEXT
    public void init(HttpSession session ){
        Context c = Context.builder().something(new HashMap<>()).build();
        session.setAttribute(CONTEXT, c);
    }

    /**
     * 
     * @param session
     * @param configKeys
     *  configkeys,是要从CONTEXT中拿出的参数KEYS字符串，使用逗号隔开
     *  HashMap<String, Object> params,是从CONTEXT对象中取出的参数容器
     *  也就是这个拦截器会根据configKeys取出各个对应的配置值，放入HASHMAP中，再放入SESSION中备用
     * @return
     */
    
    public boolean exec(HttpSession session, String configKeys){
        
        //Test Point--context 拦截器测试 ----------------------------------------
        //if( InterceptorTestHelper.exec( session, configKeys ) ) return true;
        //----------------------------------------------------------------------

        //检测上下文对象
        Context c = (Context)session.getAttribute(CONTEXT);
        if(c == null) return false;
        
        //拿到参数容器
        
        String[] keys = configKeys.split(",");
        List<String> tmp = new LinkedList<>();
        for(String key: keys){
            tmp.add(key);
        }

        //对KEY进行排序，如果是PUBLISHER和用户都有同一个KEY，则会用户的值覆盖掉PUBLISHER的值 
        Collections.sort(tmp);
       
        for(String configKey : tmp ){
            configKey = configKey.trim();
            Object o = c.get(configKey);
            if( o != null)
                session.setAttribute(configKey.split("[.]")[1], o);
             System.out.println(configKey.split("[.]")[1] +"==========--------------------------"+ o);
        }
       
        return true;
    }

}
