package edu.nuist.ojs.middle.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.ram.model.v20150501.CreateUserResponse.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.nuist.ojs.common.entity.Link;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.middle.stub.CallStub;

@Controller
public class LinkController {
    @Autowired
    private CallStub callStub;

    @RequestMapping("/link/{md5}")
    public void router(
        HttpServletRequest request,  
        HttpServletResponse response, 
        @PathVariable String md5,
        @RequestParam (required = false) String other
    ) throws IOException, ServletException{
        Link l = callStub.callStub("getLink", Link.class, "md5", md5);
        if(l.isClosed()) return;
        
        if( l != null ){
            String api = l.getApi();
            JSONObject obj = JSONObject.parseObject(l.getJsonData());
            switch( api ){
                case "/user/active":
                    long pid = obj.getLongValue("pid");
                    long uid = obj.getLongValue("id");
                    callStub.callStub("activeUser", User.class, "pid", pid, "id", uid, "md5", md5);
                    request.getSession().setAttribute("active", "true");

                    Publisher p = callStub.findPublisherById(pid);
                    response.sendRedirect( "/login/" + p.getAbbr() );
                    break;
                case "/review":
                    long actionid = obj.getLongValue("actionid");
                    if( other != null )
                        request.setAttribute("view", true);
                    request.getRequestDispatcher("/review/extend/"+  actionid).forward(request, response);
                    break;
                case "/payment":
                    long payid = obj.getLongValue("pid");
                    request.getRequestDispatcher("/payment/extend/"+  payid).forward(request, response);
                    break;
            }
            
        }

        return;
    }
}
