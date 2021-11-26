package edu.nuist.ojs.middle.controller;

import com.alibaba.fastjson.JSONObject;

import edu.nuist.ojs.common.entity.Journal;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.ContextInterceptor;
import edu.nuist.ojs.middle.interceptor.I18NAnnotation;
import edu.nuist.ojs.middle.interceptor.I18NInterceptor;
import edu.nuist.ojs.middle.resourcemapper.country.CountryComponent;
import edu.nuist.ojs.middle.resourcemapper.i18n.I18N;
import edu.nuist.ojs.middle.resourcemapper.journal.JorunalFileType;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalConfig;
import edu.nuist.ojs.middle.stub.CallStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
public class PageRouter {
    @Autowired
    private CountryComponent cc;

    @Autowired
    private CallStub callStub;

    @Autowired
    private ContextInterceptor contextInterceptor;


    public static final String ADMINCONSOLE = "OJS System Super Admin Console";

    
    @RequestMapping("/login/{publisher}")
    @I18NAnnotation(configPoint = "login", configKeys = "login")
    public String login(HttpServletRequest request, Model model, @PathVariable String publisher){
        model.addAttribute("publisherAbbr", publisher); //  插入出版社缩写
        String i18n = "";
        if( publisher.equals("admin")){
            i18n = I18N.EN;
            model.addAttribute("publisher", ADMINCONSOLE);
        }else{
            Publisher p = callStub.findPublisherByAbbr(publisher); // 取出版社名称
            model.addAttribute("publisher", p.getName());
            contextInterceptor.init(request.getSession());
            contextInterceptor.setPublisher(p, request.getSession());
            i18n = p.getI18n();
        }

        if( request.getSession().getAttribute("active") != null){   //此处激活返回
            model.addAttribute("active", true);
        }

        model.addAttribute(I18NInterceptor.I18N, i18n.equals(I18N.CN));
        return "login/login";
    }


    @RequestMapping("/home/{publisher}")
    @ContextAnnotation(configPoint = "home", configKeys = "u.email,u.publisherId,u.i18n,u.superUser,p.i18n,p.name,p.abbr")
    @I18NAnnotation(configPoint = "home", configKeys = "home")
    public String home(HttpServletRequest request, Model model, @PathVariable String publisher){

        Long pid = (Long)request.getSession().getAttribute("publisherId");
        String pname = ADMINCONSOLE;
        String abbr = "admin";
        if( pid == -1 ){
            request.getSession().setAttribute("name", pname);
            request.getSession().setAttribute("abbr", abbr);
        }

        try {
            ThymleafHelper.home(model, request.getSession());
            model.addAttribute(
                I18NInterceptor.I18N,
                I18N.CN.equals(request.getSession().getAttribute(I18NInterceptor.I18N)));
            return "home/home";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }

    @RequestMapping("/journal/list")
    @I18NAnnotation(configPoint = "journallist", configKeys = "journallist,home")
    @ContextAnnotation(configPoint = "journallist", configKeys = "u.i18n,u.root,p.i18n,u.email,u.publisherId,p.name,p.abbr")
    public String journalList(HttpSession session, Model model){
        model.addAttribute(
            I18NInterceptor.I18N,
            I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N)));
        model.addAttribute("root", session.getAttribute("root"));
        try {
            ThymleafHelper.home(model, session);
            model.addAttribute(
                I18NInterceptor.I18N,
                I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N)));
            return "journal/list";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }

    @RequestMapping("/journal/{pop}")
    @ContextAnnotation(configPoint = "journalpop", configKeys = "u.i18n,p.i18n")
    @I18NAnnotation(configPoint = "journalpop", configKeys = "journalpop")
    public String journalPop(HttpSession session, Model model, @PathVariable String pop){
        String i18n = ThymleafHelper.get(session, "i18n", String.class);
        model.addAttribute(I18NInterceptor.I18N, I18N.CN.equals(i18n));
        return "journal/" + pop;
    }

    
    @Autowired
    private JournalConfig jConfig;

    @Autowired
    private JorunalFileType jfileTypes;
    
    @RequestMapping("/journal/submit/{jid}")
    @I18NAnnotation(configPoint = "submit", configKeys = "submit,home")
    @ContextAnnotation(configPoint = "submit", configKeys = "u.i18n,u.root,p.i18n,u.email,u.publisherId,p.name,p.abbr")
    public String submit(
        HttpSession sessoin, 
        Model model, 
        @PathVariable long jid
    ){
        String[] submitConfigPoints = {
            JournalConfigPointEnum.Submit_Guidelines,
            JournalConfigPointEnum.Require_Reviewers_Num,
            JournalConfigPointEnum.Reviewer_Requirement,
            JournalConfigPointEnum.Submit_Requirements,
            JournalConfigPointEnum.Latex_Check,
            JournalConfigPointEnum.CoverLetter_Check,
            JournalConfigPointEnum.CoverLetter_Requirement
        };

        Journal j = callStub.callStub("journalbyid", Journal.class, "jid", jid);

        for(String  point : submitConfigPoints){
            JournalSetting js  = callStub.callStub("getSetting",JournalSetting.class,"journalId",jid,"configPoint",point);
            if( js == null ){
                js = jConfig.getConfig(point);
            }
            model.addAttribute( point.replace(" ", ""), js.getConfigContent());
        }
        model.addAttribute("fileTypes", jfileTypes.getFiletypes());
        try {
            ThymleafHelper.home(model, sessoin);
            model.addAttribute("isRoot", ThymleafHelper.get(sessoin, "root", boolean.class));
            model.addAttribute(
                I18NInterceptor.I18N,
                I18N.CN.equals(sessoin.getAttribute(I18NInterceptor.I18N)));
            contextInterceptor.setSomething("journalId", jid, sessoin);
            model.addAttribute("jtitle", j.getTitle());

            return "submit/home";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @RequestMapping("/journal/config/{jid}")
    @I18NAnnotation(configPoint = "journalconfig", configKeys = "journalconfig,home")
    @ContextAnnotation(configPoint = "journalconfig", configKeys = "u.i18n,u.root,p.i18n,u.email,u.publisherId,p.name,p.abbr")
    public String journalConfig(HttpSession session, Model model, @PathVariable long jid){
        try {
            ThymleafHelper.home(model, session);
            model.addAttribute("pid", ThymleafHelper.get(session, "publisherId", long.class));
            model.addAttribute("isRoot", ThymleafHelper.get(session, "root", boolean.class));
            model.addAttribute(
                I18NInterceptor.I18N,
                I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N)));

            model.addAttribute("submitandreview", JSONObject.toJSONString( jConfig.getConfigPoints().toArray(new String[0])) );
            contextInterceptor.setSomething("journalId", jid, session);
            return "journal/config";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }

    
    @RequestMapping("/user/{pop}")
    @ContextAnnotation(configPoint = "userpop", configKeys = "u.i18n,p.i18n")
    @I18NAnnotation(configPoint = "userpop", configKeys = "userpop")
    public String userPop(HttpSession session, Model model, @PathVariable String pop){
        String i18n = ThymleafHelper.get(session, "i18n", String.class);
        model.addAttribute(I18NInterceptor.I18N, I18N.CN.equals(i18n));
        return "user/" + pop;
    }

    
    @RequestMapping("/user/regist/{publisher}")
    @I18NAnnotation(configPoint = "userreigstpage", configKeys = "regist")
    public String userRegist(HttpSession session, Model model, @PathVariable String publisher){

        model.addAttribute("publisherAbbr", publisher); //  插入出版社缩写
        Publisher p = callStub.findPublisherByAbbr(publisher); // 取出版社名称
        if( p == null ) return "/error";

        contextInterceptor.init(session);
        contextInterceptor.setPublisher(p, session);

        model.addAttribute("publisher", p.getName());
        String i18n = p.getI18n();
        model.addAttribute("i18n",  I18N.CN.equals(i18n));
        model.addAttribute("countries", cc.getList().getCityList(i18n.equals(I18N.CN)));

        return "login/regist";
    }


    @RequestMapping("/login/active/{publisher}")
    @I18NAnnotation(configPoint = "active", configKeys = "active")
    public String activePage(HttpSession session, Model model, @PathVariable String publisher, @RequestParam String email){
        model.addAttribute("publisherAbbr", publisher); //  插入出版社缩写
        Publisher p = callStub.findPublisherByAbbr(publisher); // 取出版社名称
        if( p == null ) return "/error";

        contextInterceptor.init(session);
        contextInterceptor.setPublisher(p, session);

        model.addAttribute("publisher", p.getName());
        String i18n = p.getI18n();
        model.addAttribute("i18n",  I18N.CN.equals(i18n));

        session.setAttribute("email", email);
        session.setAttribute("publisher", publisher);
        return "login/active";
    }

    @RequestMapping("/user/resetpassword/{publisher}")
    @I18NAnnotation(configPoint = "resetpassword", configKeys = "resetpassword")
    public String resetPassword(HttpServletRequest request, Model model, @PathVariable String publisher){
        model.addAttribute("publisherAbbr", publisher); //  插入出版社缩写
        String i18n = "";
        if( publisher.equals("admin")){
            i18n = I18N.EN;
            model.addAttribute("publisher", ADMINCONSOLE);
        }else{
            Publisher p = callStub.findPublisherByAbbr(publisher); // 取出版社名称
            model.addAttribute("publisher", p.getName());//出版社的国际化
            i18n = p.getI18n();
        }
        model.addAttribute("i18n",  I18N.CN.equals(i18n) );
        return "login/resetPassword";
    }

    @RequestMapping("/journal/usermanagment")
    @I18NAnnotation(configPoint = "usermanagment", configKeys = "usermanagment,home")
    @ContextAnnotation(configPoint = "journalconfig", configKeys = "u.i18n,u.root,p.i18n,u.email,u.publisherId,p.name,p.abbr")
    public String getUsers(HttpSession session, Model model ){
        try {
            ThymleafHelper.home(model, session);
            model.addAttribute("isRoot", ThymleafHelper.get(session, "root", boolean.class));
            model.addAttribute(
                I18NInterceptor.I18N,
                I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N)));

            return "user/usermanagment";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }

    @RequestMapping("/user/profile/{publisher}")
    @I18NAnnotation(configPoint = "profile", configKeys = "home,profile")
    @ContextAnnotation(configPoint = "profile", configKeys = "u.userId,u.i18n,u.root,p.i18n,u.email,u.publisherId,p.name,p.abbr")
    public String profile(HttpSession session, Model model, @PathVariable String publisher){
       
        try {
            ThymleafHelper.home(model, session);
            model.addAttribute("isRoot", ThymleafHelper.get(session, "root", boolean.class));
            model.addAttribute(
                I18NInterceptor.I18N,
                I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N)));
            String i18n = ThymleafHelper.get(session, "i18n", String.class);
            long userid  = ThymleafHelper.get(session, "userId", long.class);
            model.addAttribute("userId", userid);
            model.addAttribute("countries", cc.getList().getCityList(i18n.equals(I18N.CN)));
            return "user/profile";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }

    @RequestMapping("/messages")
    @I18NAnnotation(configPoint = "messages", configKeys = "messages,home")
    @ContextAnnotation(configPoint = "messages", configKeys = "u.i18n,u.root,p.i18n,u.email,u.publisherId,p.name,p.abbr")
    public String getMessages(HttpSession session, Model model ){
        try {
            ThymleafHelper.home(model, session);
            model.addAttribute("isRoot", ThymleafHelper.get(session, "root", boolean.class));
            model.addAttribute(
                    I18NInterceptor.I18N,
                    I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N)));
            return "message/messages";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }

    @RequestMapping("/message/{pop}")
    @ContextAnnotation(configPoint = "messagepop", configKeys = "u.i18n,p.i18n")
    @I18NAnnotation(configPoint = "messagepop", configKeys = "messagepop")
    public String messagePop(HttpSession session, Model model, @PathVariable String pop){
        String i18n = ThymleafHelper.get(session, "i18n", String.class);
        model.addAttribute(I18NInterceptor.I18N, I18N.CN.equals(i18n));
        return "message/" + pop;
    }

    

    @RequestMapping("/test")
    @I18NAnnotation(configPoint = "test", configKeys = "test")
    public String test(HttpServletRequest request, Model model ){
        model.addAttribute(I18NInterceptor.I18N, true);
        return "test";
    }

    //拦截器出错页，要先从request中取到CONFIGTYPE, CONFIGKPOINTS, CONFIGKEYS,放到MODEL中去，再通过THEMLEAF渲染出来
    @RequestMapping("/errorPage")
    public String error(HttpServletRequest request, Model model){
        model.addAttribute("configType", request.getAttribute("configType"));
        model.addAttribute("configPoint", request.getAttribute("configPoint"));
        model.addAttribute("configKey", request.getAttribute("configKey"));
        return "error";
    }

}
