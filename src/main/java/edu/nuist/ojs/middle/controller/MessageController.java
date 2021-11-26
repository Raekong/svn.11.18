package edu.nuist.ojs.middle.controller;

import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.middle.file.COSFileDownloader;
import edu.nuist.ojs.middle.file.OOSFileDownloader;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.stub.CallStub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
public class MessageController {
    @Autowired
    CallStub callStub;

    @Autowired
    private OOSFileDownloader oDownloader;

    @Autowired
    private COSFileDownloader cDownloader;

    @Value("${uploader.platform}")
    private String cloudPlatform;

    @RequestMapping("/message/search")
    @ResponseBody
    @ContextAnnotation(configPoint = "searchMessage", configKeys = "u.userId")
    public String searchMessage(HttpSession session, @RequestParam int  pageNumber,
                                @RequestParam int pageSize,
                                @RequestParam(required = false) String sender,
                                @RequestParam(required = false)String title,
                                @RequestParam(required = false)String content){
        long id = ThymleafHelper.get(session, "userId", long.class);
        System.out.println(id);
      return callStub.callStub("searchMessages",String.class,
              "page",pageNumber,
              "size",pageSize,
              "id",id,
              "sender",sender,
              "title",title,
              "content",content
              );
    }

    @RequestMapping("message/getMessage")
    @ResponseBody
    public Message getMessage(@RequestParam long id){
        return callStub.callStub("getMessage",Message.class,"id",id);
    }
    
    @RequestMapping(value = "message/download")
    public void download(@RequestParam String appends, @RequestParam String type, HttpServletRequest request, HttpServletResponse response){
        if(cloudPlatform.equals("COS")){
            cDownloader.exec(appends, request, response);
        }else if( cloudPlatform.equals("OSS")){
            oDownloader.exec(appends, request, response);
        }
        
        return;

    }
    
}
