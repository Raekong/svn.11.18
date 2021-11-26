package edu.nuist.ojs.middle.controller.pages;

import org.springframework.web.bind.annotation.RestController;

import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.datamonitor.DataMonitorAnnotation;
import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.submit.PaperAnlysis;
import edu.nuist.ojs.middle.workflow.submit.StateMachine;
import edu.nuist.ojs.middle.workflow.submit.SubmitConfigCheckor;

import java.io.IOException;

import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSONObject;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class SubmitController {
    @Autowired
    private PaperAnlysis anlysis;

    @Autowired
    private CallStub callStub;

    @Autowired
    private MessageComponent mComponent;

    @Autowired
    private SubmitConfigCheckor checkor;

    
    @Autowired
    private StateMachine stateMachine; 
    

    @RequestMapping(value="/submit/analysisPaper")
    public JSONObject analysisPaper(
        @RequestParam String originName,
        @RequestParam String innerId
    ) throws IOException, TikaException {
        JSONObject obj = new JSONObject();
        obj.put(MessageComponent.ORIGIN, originName);
        obj.put(MessageComponent.INNERID, innerId);
        String url = mComponent.getFile(obj).getPath();
        return anlysis.getArticleParams(url, originName.toLowerCase());
    }

    @RequestMapping(value="/submit/do")
    @ContextAnnotation(configPoint = "submit", configKeys = "u.i18n,u.username,u.root,u.userId,p.i18n,u.email,s.journalId,u.publisherId,p.name,p.abbr")
    public String execSubmit(
        HttpSession session,
        @RequestParam String obj
    ) {

        long uid = ThymleafHelper.get(session, "userId", long.class);
        long jid = ThymleafHelper.get(session, "journalId", long.class);
        long pid = ThymleafHelper.get(session, "publisherId", long.class);

        String email = ThymleafHelper.get(session, "email", String.class);
        String name = ThymleafHelper.get(session, "username", String.class);


        String i18n = ThymleafHelper.get(session, "i18n", String.class);

        String[] rst = checkor.check(jid, JSONObject.parseObject(obj));
        if( rst != null ){
            return rst[ i18n.equals(I18N.CN) ? 0 : 1]; 
        }
        
        String cb = callStub.callStub("submit", String.class, "json", obj, "uid", uid, "jid", jid, "pid", pid);
        JSONObject article = JSONObject.parseObject(cb);
        long aid = article.getLong("id");

        //monitor:
        callStub.callStub("monitor", 
                String.class, 
                "data",
                MonitorDataAssembly.assembly(
                    "endpoint", "submit",
                    "pid", pid,
                    "jid", jid,
                    "sid",  article.getLong("sectionId"),
                    "aid", aid, 
                    "title", article.getString("title"),
                    "subdate", System.currentTimeMillis(),
                    "subid", uid,
                    "subemail", email,
                    "subname", name,
                    "sindex", 0
                )
        );

        stateMachine.exec(pid, jid, aid, uid, i18n);
        return "";
       
    }   
}
