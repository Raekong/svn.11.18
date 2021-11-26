package edu.nuist.ojs.middle.controller.article;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.nuist.ojs.common.entity.EmailFile;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;
import edu.nuist.ojs.middle.workflow.copyedit.CopyeditStateMachine;

@Controller
public class CopyeditController {
    @Autowired
    private CallStub callStub;

    @Autowired
    private MessageComponent mComponent;

    @Autowired
    private WorkflowMailHelper mailHelper;  

    @Autowired
    private ArticleFileHelper afHelper;  
    
    @Autowired
    private ArticleInfoHelper iHelper; 

    @Autowired
    private CopyeditStateMachine machine; 
    
    @Autowired
    private ArticleFileHelper fHelper;

    @RequestMapping("/copyedit/submitRevision")
    @ContextAnnotation(configPoint = "copyedit", configKeys = "u.userId")
    @ResponseBody
    public String submitRevision(
        HttpSession session,  
        @RequestParam long aid, 
        @RequestParam int rid, 
        @RequestParam String files
    ){
        JSONArray arr = JSONArray.parseArray(files);
        List<ArticleFile> afs = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){ 
            JSONObject o = arr.getJSONObject(i);
            ArticleFile af = ArticleFile.builder()
                            .aid(aid)
                            .innerId(o.getString("innerId"))
                            .originName(o.getString("originName"))
                            .fileType(o.getString("fileType"))
                            .version("COPYEDIT-" + ( rid+ 1) )
                            .build();
            afs.add(af);
        }

        //序列化新的一轮文件
        fHelper.serizalNewCopyeditRoundFiles(aid, rid, afs);

        //切换HISTORY到新的一轮
        long uid = ThymleafHelper.get(session, "userId", long.class);
        machine.newRound(aid, rid, uid);
        return "";
    }

    @RequestMapping("/copyedit/decision/pass")
    @ContextAnnotation(configPoint = "copyedit", configKeys = "u.userId")
    @ResponseBody
    public void pass(   HttpSession session,  @RequestParam long aid,  @RequestParam int rid ){
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        machine.pass(aid,uid,rid);
    }

    @RequestMapping("/copyedit/sendDecision")
    @ContextAnnotation(configPoint = "copyedit", configKeys = "u.userId")
    @ResponseBody
    public String sendDecision(
        HttpSession session, 
        @RequestParam long jid, 
        @RequestParam long aid, 
        @RequestParam int rid, 
        @RequestParam int recvType, 
        @RequestParam String type,
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam String attachfiles
    ){
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        JSONArray attachments = JSONArray.parseArray(attachfiles);
        //发送通过邮件
        long mid = mailHelper.sendDecisionMail(jid, aid, recvType, type, title, content, attachments);
        //通过MACHINE来调整历史
        switch(type){
            case "accept":
                // machine.accept(mid, rid, uid, aid, jid, pagenum, linkmd5, nextStageFiles);
                break;
            case "revision":
                machine.revision(mid, rid, uid, aid);
                break;
        }
        return "";
    }

    @RequestMapping("/copyedit/decision/revision")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.email,u.i18n,u.root,p.i18n,p.id")
    public String revision(
        HttpSession session, 
        Model model,
        @RequestParam long aid, 
        @RequestParam long jid, 
        @RequestParam int rid 
    ){
        model.addAttribute("roundFiles", afHelper.getFileForReviewRound(aid, rid));
        model.addAttribute("type", "revision");
        return "article/tab/copyedit-decision";
    }
    
    @RequestMapping("/article/copyedit/sendmessage")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.email,u.i18n,u.root,p.i18n,p.id")
    @ResponseBody
    public String sendMessage(
        HttpSession session,  
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam String attachments,
        @RequestParam long rid,
        @RequestParam long aid,
        @RequestParam boolean isAuthor
    ){
        String name = ThymleafHelper.get(session, "username", String.class);
        long pid = ThymleafHelper.get(session, "id", long.class);
        String email = ThymleafHelper.get(session, "email", String.class);
        long userId = ThymleafHelper.get(session, "userId", long.class);

        List<EmailFile> attachmentFiles = new LinkedList<>();  
    
        JSONArray files = JSONObject.parseArray(attachments);
        for(int i=0; i<files.size(); i++){
            EmailFile f = mComponent.getFile(files.getJSONObject(i)); 
            attachmentFiles.add(f);
        }

        User recevierUser = User.builder()
            .email("Default")
            .username("Default")
            .build();
        //用CONFIGPOINT的位置放了发件人
        Message m = mComponent.SendJournalMessage(
                false, 
                pid, 
                recevierUser, 
                name,
                title, 
                content, 
                attachmentFiles 
        );

        callStub.callStub("savediscuss", 
                String.class, 
                "aid" , aid, 
                "rid", rid, 
                "sendid", userId, 
                "sendEmail", email,
                "receId", -1,
                "msgid", m.getId(),
                "type", 1
        );
        
        //如果是编辑发送信息,则按发送邮件通知作者去登录
        if( !isAuthor ){
            HashMap<String, String> info = iHelper.getVariableMap(aid); 
            mailHelper.sendMessageAuto(null , info, "Editor Discuss Notify", null); 
        }
        return "";
    }
}
