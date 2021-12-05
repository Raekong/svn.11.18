package edu.nuist.ojs.middle.controller.pages;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.context.Context;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.ContextInterceptor;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.stub.CallStub;
import org.mockito.MockingDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    @ResponseBody
    @ContextAnnotation(configPoint = "userturn", configKeys = "u.userId,p.id")
    public JSONObject turnUser(HttpSession session,
                         @RequestParam long id
    ) {
        JSONObject rst = new JSONObject();
        //保存原用户的context为temp，存在session中
        Context temp = ThymleafHelper.getContext(session);
        session.setAttribute("tempContext",temp);

        //新用户登录
        long pid = ThymleafHelper.get(session, "id", long.class);
        User u = callStub.getUserById(id);
        u = callStub.loginAs(u.getEmail(), u.getPassword(), pid);

        //更新新用户的Context
        String abbr = (String) session.getAttribute("publisher");
        Publisher p = callStub.findPublisherByAbbr(abbr);

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
        }
        session.setAttribute("loginAs", true);

        rst.put("abbr", abbr);
        return rst;
    }

    @RequestMapping("/user/turnBack")
    @ResponseBody
    public JSONObject turnBackUser(HttpSession session){
        Context temp = (Context) session.getAttribute("tempContext");
        session.setAttribute("Context", temp);

        session.setAttribute("loginAs", false);
//        session.removeAttribute("loginAs");
        return new JSONObject();
    }
}
