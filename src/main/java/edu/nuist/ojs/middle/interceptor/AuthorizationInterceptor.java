package edu.nuist.ojs.middle.interceptor;

import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.middle.context.Context;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.HashSet;

/**
 * 检查权限操作
 *      在AuthorizationAnnotation中有字段定义所需要的权限
 * 
 * 检查的方法是：
 *      1. 从CONTEXT中取出当前操作的JOURNAL，以及USER，
 *      2. 在USER中根据当前JOURNAL ID从roles中取出该用户可以拥有的ROLE角色
 *      3. 遍历用户角色列表，标注所需要的权限作对比，如果命中，返回TRUE，如果没有命中，则返回FALSE 
 */
@Component
public class AuthorizationInterceptor extends Interceptor {
    public boolean exec(HttpSession session, String role){
       
       return false;
    }
}
