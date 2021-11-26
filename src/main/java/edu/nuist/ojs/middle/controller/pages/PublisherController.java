package edu.nuist.ojs.middle.controller.pages;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.controller.PageRouter;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.I18NAnnotation;
import edu.nuist.ojs.middle.interceptor.I18NInterceptor;
import edu.nuist.ojs.middle.stub.CallStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class PublisherController {
    @Autowired
    private CallStub callStub;
    
    @RequestMapping("/publiser/{page}")
    @ContextAnnotation(configPoint = "homemenu", configKeys = "u.email,u.publisherId,u.i18n,p.i18n,u.superUser")
    @I18NAnnotation(configPoint = "publisher", configKeys = "home,publisherlist,configpublisher,newpublisher")
    public String pages(HttpServletRequest request, @PathVariable String page, Model model) throws Exception{
        
        String html = "";
        //为配置publisher设置的值，从config那边转过来的
        if( request.getAttribute("p") != null){ 
            model.addAttribute("p",  request.getAttribute("p"));
        }
        
        switch(page){
            case "create":
                html = "publisher/create";
                break;
            case "management":
                String name = request.getParameter("name");
                String abbr = request.getParameter("abbr");
                if(!"".equals(name)) model.addAttribute("name", name);
                if(!"".equals(abbr)) model.addAttribute("abbr", abbr);
                html = "publisher/list";
                break;
        }

        model.addAttribute(
                I18NInterceptor.I18N,
                I18N.CN.equals(request.getSession().getAttribute(I18NInterceptor.I18N)));
        request.getSession().setAttribute("name", PageRouter.ADMINCONSOLE);
        request.getSession().setAttribute("abbr", "admin");

        ThymleafHelper.home(model, request.getSession());

        return html;
    }

    @RequestMapping("/publisher/canPayment")
    @ResponseBody
    public String canPayment(@RequestParam long pid){
        Publisher p = callStub.findPublisherById(pid);
        if( p.getPaymentSetting() != null){
            JSONObject payment = JSONObject.parseObject(p.getPaymentSetting());
            if( payment.keySet().size()>0){
                return "true";
            }
        }
        return "false";
    }


    @RequestMapping("/publisher/config/{id}")
        public void config(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable long id, Model model
        ) throws ServletException, IOException{
        Publisher p = callStub.findPublisherById(id);
        request.setAttribute("p", JSON.toJSONString(p));
        request.getRequestDispatcher("/publiser/create").forward(request, response);
    }

    
    @RequestMapping("/publisher/list")
    @ResponseBody
    public String getall(
        @RequestParam(required = false) String  name,
        @RequestParam(required = false) String  abbr,
        @RequestParam("pageNumber") int  pageNumber,
        @RequestParam("pageSize") int pageSize
    ){
        if(name == null) name = "";
        if(abbr == null) abbr = "";
        return callStub.searchPublishers(pageNumber, pageSize, name,  abbr);
    }

    @RequestMapping("/publisher/update")
    @ResponseBody
    public Publisher updataPublisher(
        @RequestParam long id,
        @RequestParam String name,
        @RequestParam String abbr,
        @RequestParam String host,
        @RequestParam int port,
        @RequestParam String password,
        @RequestParam String email,
        @RequestParam String emailsender,
        @RequestParam String lang,
        @RequestParam String modules,
        @RequestParam String paymentSetting,
        @RequestParam String address){

            Publisher p = new Publisher();
            p.setId(id);       p.setName(name);            p.setAbbr(abbr);      p.setEmailSender(emailsender);   p.setHost(host);    
            p.setPassword(password);  p.setEmailAddress(email);   p.setPaymentSetting(paymentSetting);   p.setPort(port);      p.setI18n(lang);    p.setModuleJson(modules);   
            p.setContact(address);    
            
            System.out.println(p);
            return callStub.updatePublisher(p);
    }

    @RequestMapping("/publisher/regist")
    @ResponseBody
    public Publisher registPublisher(
        @RequestParam String name,
        @RequestParam String abbr,
        @RequestParam String host,
        @RequestParam int port,
        @RequestParam String password,
        @RequestParam String email,
        @RequestParam String emailsender,
        @RequestParam String lang,
        @RequestParam String modules,
        @RequestParam String paymentSetting,
        @RequestParam String address,
        @RequestParam String  rootemail,
        @RequestParam String  rootname,
        @RequestParam( required = false, defaultValue = ""  ) String rootpassword
    ){
        Publisher p = new Publisher();
        p.setName(name);            p.setAbbr(abbr);      p.setEmailSender(emailsender);   p.setHost(host);    p.setPassword(password);   
        p.setEmailAddress(email);   p.setPort(port);      p.setI18n(lang);    p.setPaymentSetting(paymentSetting);  p.setModuleJson(modules);   p.setContact(address);
        

       User u = User.builder()
               .isActived(true)
               .password(("".equals(rootpassword)||rootpassword==null||"null".equals(rootpassword)||"undefined".equals(rootpassword)) ? "888888":rootpassword)
               .email(rootemail)
               .i18n(p.getI18n())
               .root(true)
               .isActived(true)
               .username(rootname)
               .build();
        return callStub.newPublisher(p, u);
    }

    @RequestMapping("publisher/disable")
    @ResponseBody
    public Publisher disable(@RequestParam long id){
        return callStub.publisherDisable(id);
    }

    @RequestMapping("publisher/active")
    @ResponseBody
    public Publisher enable(@RequestParam long id){
        return callStub.publisherEnable(id);
    }
}
