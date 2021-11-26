package edu.nuist.ojs.middle.controller.article;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.nuist.ojs.common.entity.EmailConfigPoint;
import edu.nuist.ojs.common.entity.EmailTpl;
import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.middle.resourcemapper.emailtpl.EmailTplMapper;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;

@Controller
public class ArticleEmailController {
    @Autowired
    private CallStub callStub;

    @Autowired
	public EmailTplMapper systemEmailConfig; 

    @Autowired
	public ArticleInfoHelper infoHelper;

    public HashMap<String, String> JsonObjectToHashMap(JSONObject jsonObj){
        HashMap<String, String> data = new HashMap<String, String>(); 
        for( String key: jsonObj.keySet()){
            data.put(key, jsonObj.getString(key));
        }
        
        return data;
    }

    @RequestMapping("/article/email")
    public String getEmailSetting( 
        Model model, 
        @RequestParam String configPoint,
        @RequestParam String i18n, 
        @RequestParam long jid,
        @RequestParam long aid,
        @RequestParam( required = false ) String attachInfos
    ){

        String obj = callStub.callStub("getEmailConfigForJournal", String.class, "jid", jid, "configPoint", configPoint);
        JSONObject tmp = JSONObject.parseObject(obj);
        EmailConfigPoint ecp = null;
        if( tmp == null ) {
            ecp =  systemEmailConfig.getEmailConfigPoint(configPoint);
            ecp.getTpls().get(0).setDefaultTpl(true);
        }else{
            ecp = new EmailConfigPoint(tmp);
            ecp.getTpls().addAll(systemEmailConfig.getEmailConfigPoint(configPoint).getTpls());
        }

        HashMap<String, String> infos = infoHelper.getVariableMap(aid);
        if( attachInfos != null ){
             infos.putAll(JsonObjectToHashMap( JSONObject.parseObject(attachInfos)));
        }
        System.out.println("=============------------------------------");
        System.out.println(ecp);


        List<JSONObject> tplNames = new LinkedList<>();  
        for(EmailTpl tpl : ecp.getTpls()){
            JSONObject t = new JSONObject();
            t.put("tplId", tpl.getId());
            t.put("isDefault", tpl.isDefaultTpl());
            t.put("tplName", tpl.getName());
            t.put("recvtype", tpl.getRecipient());
            tplNames.add( t );
        }
        
        List<JSONObject> tpls = new LinkedList<>();  
        for(EmailTpl tpl : ecp.getTpls()){
            JSONObject t = new JSONObject();
            t.put("tplId", tpl.getId());
            t.put("tplName", tpl.getName());
            t.put("title",  tpl.renderTitle(infos, i18n.equals(I18N.CN), true));
            t.put("body",  tpl.render(infos, i18n.equals(I18N.CN), true));
            tpls.add(t);
        }

        if(
            configPoint.equals("Decline Submission") 
            || configPoint.equals("Article Accept") 
            || configPoint.equals("Article Revision")
            || configPoint.equals("Similar Checked Revision")
            || configPoint.equals("Similar Checked Decline")
            || configPoint.equals("Copyedit Requirement")
            
        ){
            model.addAttribute("decision", true);
        }
        
        model.addAttribute("attributeName", ecp.getConfigPoint());
        model.addAttribute("tplNames", tplNames);
        model.addAttribute("tpls", tpls);
        model.addAttribute("i18n", i18n);
        return "article/emailtpl";
    }
}
