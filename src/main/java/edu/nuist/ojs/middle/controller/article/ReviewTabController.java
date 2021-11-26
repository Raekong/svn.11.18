package edu.nuist.ojs.middle.controller.article;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.nuist.ojs.common.entity.EmailFile;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;
import edu.nuist.ojs.middle.workflow.JournalUserRoleHelpler;
import edu.nuist.ojs.middle.workflow.WorkFlowMainStateMachine;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;
import edu.nuist.ojs.middle.workflow.review.ReviewStateMachine;
import edu.nuist.ojs.middle.workflow.review.ReviewTabHelper;

@RestController
public class ReviewTabController {

    @Autowired
    private ReviewStateMachine machine;

    @Autowired
    private WorkFlowMainStateMachine flowMachine;

    @Autowired
    private MessageComponent mComponent;
    
    @Autowired
    private ArticleUserRoleHelper arHelper;

    @Autowired
    private CallStub callStub;

    @Autowired
    private JournalUserRoleHelpler jrHelper;

    @Autowired
    private WorkflowMailHelper mailHelper; 

    @Autowired
    private ArticleInfoHelper iHelper; 

    @Autowired
    private ReviewTabHelper tHelper;  

    @Autowired
    private ArticleFileHelper fHelper;

    @RequestMapping("/review/submitRevision/")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public void submitRevision(
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
                            .version("REVIEW-" + ( rid+ 1) )
                            .build();
            afs.add(af);
        }

        //序列化新的一轮文件
        fHelper.serizalNewReviewRoundFiles(aid, rid, afs);

        //切换HISTORY到新的一轮
        long uid = ThymleafHelper.get(session, "userId", long.class);
        machine.newRound(aid, rid, uid);
    }
    

    @RequestMapping("/review/sendDecision")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String decline(
        HttpSession session, 
        @RequestParam long jid, 
        @RequestParam long aid, 
        @RequestParam int rid, 
        @RequestParam int recvType, 
        @RequestParam String type,
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam String attachs,
        @RequestParam (required = false) String nextStageFiles,
        @RequestParam (required = false) Integer pagenum,
        @RequestParam (required = false) String linkmd5
    ){
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        JSONArray attachments = JSONArray.parseArray(attachs);
        //发送通过邮件
        long mid = mailHelper.sendDecisionMail(jid, aid, recvType, type, title, content, attachments);
        //通过MACHINE来调整历史
        switch(type){
            case "decline":
                machine.decline(mid, rid, uid, aid);
                break;
            case "accept":
                machine.accept(mid, rid, uid, aid, jid, pagenum, linkmd5, nextStageFiles);
                break;
            case "revision":
                machine.revision(mid, rid, uid, aid);
                break;
        }
        //关闭所有未结束的审稿，并发邮件通知
        String jsonArr = callStub.callStub("unCompleteActionByAidAndRid", String.class, "aid", aid, "rid", rid);
        JSONArray unclosedActions = JSONArray.parseArray(jsonArr);
        for(int i=0; i<unclosedActions.size(); i++){
            long raid = unclosedActions.getJSONObject(i).getLong("id");
            callStub.callStub("reveiwactionclose", String.class,  "raid", raid);
            mailHelper.sendCloseRemind( aid, raid );
        }

        return "";
    }

    @RequestMapping("/article/review/passReview")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String passReview(HttpSession session, @RequestParam long aid){
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        machine.previewPassed(uid, aid);
        return "";
    }

    @RequestMapping("/article/review/suggest")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String suggest(HttpSession session, @RequestParam long aid, @RequestParam int rid,  @RequestParam String suggest){
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        machine.suggest( rid,  uid, aid, suggest );
        return "";
    }

    //设定用户转刊
    @RequestMapping("/article/review/change")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String change(HttpSession session, @RequestParam long jid, @RequestParam long sid, @RequestParam int rid, @RequestParam long aid){
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        machine.changeJournal(  jid,  sid,  aid,  uid,  rid );
        return "";
    }

    @RequestMapping("/article/review/changedo")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String changeDo(HttpSession session, @RequestParam long jid, @RequestParam long sid, @RequestParam int aid){
        flowMachine.changeJournalDo(  jid,  sid,  aid );
        return "";
    }

    @RequestMapping("/article/review/addEditor")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String addEditor(HttpSession session, @RequestParam boolean authority,  @RequestParam long uid, @RequestParam int aid){
        arHelper.saveEditor(aid, 5, uid, authority); //section role id == 5
        return "";
    }

    @RequestMapping("/article/review/delEditor")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String delEditor(HttpSession session, @RequestParam String email, @RequestParam int aid){
        arHelper.delEditor(aid, 5, email); //section role id == 5
        return "";
    }


    @RequestMapping("/article/review/queryReviewer")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.username,u.i18n,u.root,p.i18n,p.id")
    public String queryReviewer(
        HttpSession session, 
        @RequestParam String name,
        @RequestParam String email,
        @RequestParam String affiliation,
        @RequestParam String research,
        @RequestParam String pageNumber,
        @RequestParam String pageSize
    ){
        long pid = ThymleafHelper.get(session, "id", long.class);
        return callStub.callStub(
                "queryReviewer", 
                String.class, 
                "name", name, 
                "email", email, 
                "affiliation", affiliation, 
                "research", research, 
                "pid", pid,
                "page", pageNumber, 
			    "size", pageSize
        );
    }   

    
    @RequestMapping("/review/sendRemind/{raid}")
    public String sendRemind( @PathVariable long raid ){
        mailHelper.sendReviewMail(raid, "Review Remind");
        //更新提醒记录
        callStub.callStub("updateRemindCount", String.class, "raid", raid, "isSystem", false);
        return "";
    }


    @RequestMapping("/article/review/sendReviewRequest")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.username,u.i18n,u.root,p.i18n,p.id")
    public String sendReviewRequest(HttpSession session,  
        @RequestParam long jid,
        @RequestParam long aid,
        @RequestParam int rid,
        @RequestParam String data
    ){
        JSONObject obj = JSONObject.parseObject(data);
        String title = obj.getString("title");
        String content = obj.getString("content");

        //每个REVIEWER JSON中带有参数
        /**
         *  affiliation: "-"
            email: "2645145487@qq.com"
            name: "zhao hou"
            researchfield: "-"
            responseDue: "2021-09-07"
            reviewDue: "2021-09-10"
         */
        long pid = ThymleafHelper.get(session, "id", long.class);
        long uid = ThymleafHelper.get(session, "userId", long.class);
     
        //序列化REVIEWER,交给TABHELPER完成，必须要在这里先保存REVIEWER后面再保存REVIEW ACTION，因为会用到这个REVIEWER 的ID
        JSONArray reviewers = obj.getJSONArray("reviewers");
        tHelper.saveReviewers(pid, reviewers);
    
        //序列化REVIEW ACTION，在每个JSONOBJECT对象中插入ACTION ID, 这一次做了调整，把审稿文件放在审稿活动中，更顺一点
        JSONArray arrf = obj.getJSONArray("attaches");
        tHelper.saveReviewAction(pid, aid, rid, uid, reviewers, arrf);

        //发送审稿链接，这个很重要，每个审稿人的链接都不一样，审稿页面也要做验证
        //在各个JSON中插入审稿链接，为邮件中使用
        tHelper.saveReviewActionLinks(aid, reviewers);

        //发送邮件中不能附AID，应该附带的是ACTION 的ID，这样才比较方便
        //发送审稿邀请邮件，有几个参数要替换，1. #Reviewers Name#,#Review Response Date#  #Review Due Date#. #accessUrl#
        mailHelper.sendReviewInviteMail(jid, aid, title, content, reviewers, arrf);

        //查看是否需要系统自动提醒，如果需要，则要待提醒的记录插入数据库
        //此时ACTION已经保存，并插入在reviewers的JSONOBJCT中，obj.put("actionId", actionId);
        JournalSetting  notify = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid, "configPoint","Review OverDue Notify");
        if( notify!=null && "true".equals(notify.getConfigContent())){
            JournalSetting  times = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid, "configPoint","Review OverDue Notify Time");
            JournalSetting  pre = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid, "configPoint","Review OverDue Notify Period");
        
            for(int i=0; i<reviewers.size(); i++){
                JSONObject tmp = reviewers.getJSONObject(i);
                callStub.callStub("saveReviewActionRemind", String.class, "raid", tmp.getLong("actionId"), "times", times.getConfigContent(), "pre", pre.getConfigContent());
            }
        }

        //一旦发出要切换到UNDER REVIEWING状态下
        machine.sendReview(uid, aid, rid);

        //monitor: 创建REVIEW Round;
        callStub.callStub("monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "reviewround",
                "aid", aid,
                "pid", pid,
                "seq", rid,
                "reviewers", JSON.toJSONString(reviewers),
                "total", reviewers.size(),
                "status", 3
            )
        );

        return null;    
    }

    @RequestMapping("/article/review/sendDisscuss")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.email,u.username,u.i18n,u.root,p.i18n,p.id")
    public String sendDisscuss(HttpSession session,  
        @RequestParam String title,
        @RequestParam boolean isEmail,
        @RequestParam String content,
        @RequestParam String attachments,
        @RequestParam long rid,
        @RequestParam long aid,
        @RequestParam boolean isAuthor
    ){
        String name = ThymleafHelper.get(session, "username", String.class);
        String email = ThymleafHelper.get(session, "email", String.class);
        long pid = ThymleafHelper.get(session, "id", long.class);

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
                "sendid", -1, 
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

    @RequestMapping("/article/review/sendmessage")
    @ContextAnnotation(configPoint = "review", configKeys = "u.userId,u.email,,u.i18n,u.root,p.i18n,p.id")
    public String sendMessage(HttpSession session,  
        @RequestParam String title,
        @RequestParam boolean isEmail,
        @RequestParam String content,
        @RequestParam String recevier,
        @RequestParam String attachments,
        @RequestParam long rid,
        @RequestParam long aid,
        @RequestParam long recvid,
        @RequestParam int type
    ){
            long userid = ThymleafHelper.get(session, "userId", long.class);
            long pid = ThymleafHelper.get(session, "id", long.class);
            String email = ThymleafHelper.get(session, "email", String.class);
            
            JSONObject utmp = JSONObject.parseObject(recevier); 
            User recevierUser = User.builder()
                                .email(utmp.getString("email"))
                                .username(utmp.getString("name"))
                                .build();
    
            List<EmailFile> attachmentFiles = new LinkedList<>(); 
    
            JSONArray files = JSONObject.parseArray(attachments);
            for(int i=0; i<files.size(); i++){
                EmailFile f = mComponent.getFile(files.getJSONObject(i));
                attachmentFiles.add(f);
            }

            //用CONFIGPOINT的位置放了发件人
            User sender = jrHelper.getEditor(userid);
            Message m = mComponent.SendJournalMessage(
                isEmail, 
                pid, 
                recevierUser, 
                sender.getUsername(), 
                title, 
                content, 
                attachmentFiles 
            );


            callStub.callStub("savediscuss", 
                String.class, 
                "aid" , aid, 
                "rid", rid, 
                "sendid", userid, 
                "sendEmail", email,
                "receId", recvid,
                "msgid", m.getId(),
                "type", type
            );
    
        return "";
    }

 
}
