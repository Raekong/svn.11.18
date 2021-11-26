package edu.nuist.ojs.middle.controller.pages;


import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.Journal;
import edu.nuist.ojs.common.entity.journalsetting.JournalRole;
import edu.nuist.ojs.middle.context.Context;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.ContextInterceptor;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalRoleMenuSetting;
import edu.nuist.ojs.middle.stub.CallStub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
public class HomeController {
    @Autowired
    private JournalRoleMenuSetting jMenuSetting;

    @Autowired
    private CallStub callStub;

    @Autowired
    private ContextInterceptor contextInterceptor;

    @ResponseBody
    @RequestMapping("/home/submit")
    @ContextAnnotation(configPoint = "submit", configKeys = "u.i18n,p.i18n,u.superUser,u.root,p.id,p.abbr" )
    public JSONObject getJournalsForSubmit(HttpSession session){
        Long pid = ThymleafHelper.get(session, "id", Long.class);
        boolean root = ThymleafHelper.get(session, "root", Boolean.class);
        List journals = callStub.callStub("journallist", List.class, "pid", pid);

        //返回类型转换，不转换会错
        ObjectMapper mapper = new ObjectMapper();
        List<Journal> list = mapper.convertValue(journals, new TypeReference<List<Journal>>() { });

        JSONObject obj = new JSONObject();
        obj.put("lang",  ThymleafHelper.get(session, "i18n", String.class));
        obj.put("list", list);
        return obj;
    }
    
    @ResponseBody
    @RequestMapping("/home/menu")
    @ContextAnnotation(configPoint = "homemenu", configKeys = "u.i18n,p.i18n, u.superUser,u.root,p.abbr" )
    public JSONObject getHomeMenus(HttpServletRequest request, Model model){
        String i18n = (String)request.getSession().getAttribute("i18n");
        boolean superUser = (boolean)request.getSession().getAttribute("superUser");
        boolean root = ThymleafHelper.get(request.getSession(), "root", boolean.class);
        String abbr = ThymleafHelper.get(request.getSession(), "abbr", String.class);

        JSONObject obj = new JSONObject();
        if(superUser){
            obj.put("menu", jMenuSetting.getMenusForSuper(i18n.equals(I18N.CN)));
            obj.put("abbr", "admin");
            return obj;
        }else if(root){ 
            obj.put("menu", jMenuSetting.getMenusForRoot(i18n.equals(I18N.CN)));
            obj.put("abbr", abbr);
            return obj;
        }

        Context c = (Context) request.getSession().getAttribute(ContextInterceptor.CONTEXT);
        JournalRole role = c.getRoleByChargeLevel();
        obj.put("menu", jMenuSetting.getMenusByRole(role, i18n.equals(I18N.CN)));
        obj.put("abbr", abbr);
        return obj;
    }


}
