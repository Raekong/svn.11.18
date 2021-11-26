package edu.nuist.ojs.middle.controller.article;

import com.alibaba.fastjson.JSON;
import edu.nuist.ojs.common.entity.SimilarCheck;
import edu.nuist.ojs.middle.stub.CallServiceUtil;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;
import edu.nuist.ojs.middle.workflow.similarcheck.SimilarCheckStateMachine;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;

@Controller
public class SimilarCheckController {

    @Autowired
    CallStub callStub;    

    @Autowired
    private CallServiceUtil callServiceUtil;

    @Autowired
    private JournalSettingHelper jsHelper; 

    @Autowired
    private WorkflowMailHelper mailHelper;  

    @Autowired
    private SimilarCheckStateMachine machine; 

    @Value("${global.simialcheck.folderId}")
    private String folderId;

    @RequestMapping("/similarCheck/begin")//确认
    @ResponseBody
    public SimilarCheck start(
        @RequestParam long aid,
        @RequestParam String title,
        @RequestParam String link,
        @RequestParam String name
    ){
        SimilarCheck sc = SimilarCheck.builder()
                        .aid(aid)
                        .title(title)
                        .fileName(name)
                        .round(0)
                        .link(link).build();
        return callStub.callStub("saveSimilarCheck", SimilarCheck.class, "similarCheck", sc);
    }
    
    @RequestMapping("/similarCheck/upload")//上传到iten
    @ResponseBody
    public SimilarCheck upload(@RequestParam long aid){
        List<SimilarCheck> similarChecks = JSON.parseArray(callStub.callStub("findSimilarCheckByAid", String.class,"aid",aid), SimilarCheck.class);
        if (similarChecks.size()==0)return null;
        Map<String,Object> data=new HashMap<String, Object>();
        data.put("folderId", folderId);
        data.put("link", similarChecks.get(0).getLink());
        data.put("authorFirstName","");
        data.put("authorLastName","");
        data.put("documentTitle","test");
        data.put("fileName","test.docx");
        String documentId= callServiceUtil.callService("report","upload",data,String.class,"reportServerRouter");
        System.out.println(documentId);
        similarChecks.get(0).setCheckid(documentId);
        return callStub.callStub("saveSimilarCheck",SimilarCheck.class,"similarCheck",similarChecks.get(0));
    }


    @RequestMapping("/similarCheck/findByAid")
    @ResponseBody
    public List<SimilarCheck> findSimilarCheckByAid(@RequestParam long aid){
        return callStub.callStub("findSimilarCheckByAid",List.class,"aid",aid);
    }

    @RequestMapping("/similarCheck/findByAidAndRound")
    @ResponseBody
    public SimilarCheck findSimilarCheckByAidAndRound(@RequestParam long aid,
                                                      @RequestParam int round){
        return callStub.callStub("findSimilarCheckByAidAndRound", SimilarCheck.class, "aid", aid,"round",round);
    }

    @RequestMapping("/similarcheck/revision/upload")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    @ResponseBody
    public void revisionUpload(
        @RequestParam long jid, 
        @RequestParam long aid, 
        @RequestParam int rid, 
        @RequestParam String fileType,
        @RequestParam String originName,
        @RequestParam String innerId
    ){
        machine.revisionUpload(jid, aid, rid, fileType, originName, innerId);
        return;
    }

    @RequestMapping("//similarcheck/sendToSimilarCheck")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    @ResponseBody
    public void sendToSimilarCheck(
        HttpSession session, 
        @RequestParam long jid, 
        @RequestParam long aid, 
        @RequestParam int rid
    ){
        long uid = ThymleafHelper.get(session, "userId", long.class);
        machine.sendToSimilarCheck(jid, aid, rid, uid);
        return;
    }


    @RequestMapping("/similarcheck/decision/{type}")
    public String decision(
        Model model, 
        @RequestParam long jid, 
        @PathVariable String type
    ){
        List<JournalSetting> jss = jsHelper.getAllSettingForJounral(jid);
        for(JournalSetting js : jss){
            if( js.getConfigPoint().equals(JournalConfigPointEnum.Similarity_Revision_Due)){
                model.addAttribute("revision_due", js.getConfigContent());
            }
        }
        model.addAttribute("decisiontype", type);
        return "article/tab/similarcheck-email";
    }

    @RequestMapping("/similarcheck/decision/done/")
    @ResponseBody
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String decisionDon(
        HttpSession session, 
        @RequestParam long jid, 
        @RequestParam long aid, 
        @RequestParam int rid, 
        @RequestParam int recvType, 
        @RequestParam String type,
        @RequestParam String title,
        @RequestParam String content
    ){

        long uid = ThymleafHelper.get(session, "userId", long.class);
        //发送通过邮件
        long mid = mailHelper.sendDecisionMail(jid, aid, recvType, type, title, content, null);
        //通过MACHINE来调整历史
        switch(type){
            case "decline":
                machine.decline(mid, rid, uid, aid);
                break;
            case "pre-review decline":
                machine.previewdecline(mid, rid, uid, aid);
                break;
            case "revision":
                machine.revision(mid, rid, uid, aid);
                break;
        }
        
        return "";
    }
}
