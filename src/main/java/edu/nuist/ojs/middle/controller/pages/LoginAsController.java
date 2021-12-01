package edu.nuist.ojs.middle.controller.pages;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.context.Context;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.ContextInterceptor;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.stub.CallStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Enumeration;

/**
 * @author Raekong
 * @create 2021-12-01 15:57
 */
@Controller
public class LoginAsController {
    @Autowired
    private CallStub callStub;

    @Autowired
    private ContextInterceptor contextInterceptor;

    @Autowired
    private JournalSettingHelper jsh;
    @RequestMapping("/user/turn")
    @ContextAnnotation(configPoint = "userturn", configKeys = "u.userId,p.id")
    public User turnUser(HttpSession session,
                         @RequestParam long id

    ) {
        //保存原用户的context为temp，存在session中
        Context temp = ThymleafHelper.getContext(session);
        session.setAttribute("tempContext",temp);
        System.out.println("id = " + id);
        //新用户登录
        long pid = ThymleafHelper.get(session, "id", long.class);
        User u = callStub.getUserById(id);
        System.out.println("u = " + u);
        u = callStub.loginAs(u.getEmail(), u.getPassword(), pid);

        //更新新用户的Context
        String abbr = (String) session.getAttribute("publisher");
        Publisher p = callStub.findPublisherByAbbr(abbr);

        //查看session中所有值
        System.out.println("替换context前");
        Enumeration<String> attrs = session.getAttributeNames();
        // 遍历attrs中的
        while(attrs.hasMoreElements()){
            // 获取session键值
            String name1 = attrs.nextElement().toString();
            // 根据键值取session中的值
            Object vakue = session.getAttribute(name1);
            // 打印结果
            System.out.println("------" + name1 + ":" + vakue +"--------\n");
        }


        if (u != null && u.getUserId() != 0) {
            if (u.isActived()) {
                contextInterceptor.init(session);
                contextInterceptor.setUser(u, session);

                if (p != null) {
                    contextInterceptor.setPublisher(p, session);
                    String jsonstr = callStub.callStub("journalrole", String.class, "uid", u.getUserId());
                    contextInterceptor.setRoles(jsh.getRoleForUser(jsonstr, u.getEmail()), session);
                }
            }
            System.out.println("替换context后");
            Enumeration<String> attrs1 = session.getAttributeNames();
            // 遍历attrs中的
            while(attrs1.hasMoreElements()){
                String name1 = attrs1.nextElement().toString();
                Object vakue = session.getAttribute(name1);
                System.out.println("------" + name1 + ":" + vakue +"--------\n");
            }
        }
        return u;
    }

    @RequestMapping("/user/turnBack")
    public JSONObject turnBackUser(HttpSession session){
        Context temp = (Context) session.getAttribute("tempContext");
        session.setAttribute("Context", temp);
        return new JSONObject();
    }
}
