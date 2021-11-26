package edu.nuist.ojs.middle.workflow;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.EmailTpl;
import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.Link;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.Article;
import edu.nuist.ojs.common.entity.article.ArticleAuthor;
import edu.nuist.ojs.common.entity.review.ReviewAction;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.email.MessageTplComponent;
import edu.nuist.ojs.middle.resourcemapper.emailtpl.EmailRecvEnum;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class WorkflowMailHelper {

    @Autowired
    private CallStub callStub;

    @Autowired
    private MessageTplComponent mTplComponent;

    @Autowired
    private MessageComponent mComponent; 

    @Autowired
    private ArticleInfoHelper iHelper;  

    @Autowired
    private JournalUserRoleHelpler jsHelper;


    public static final int PUBLISHER_MAIL = 1;
    public static final int JOURNAL_MAIL = 2;
    public static final int USER_MAIL = 3;  

    public String render(String content, HashMap<String, String> model){
        String tmp = content;
        for(Entry<String, String> entry : model.entrySet()) {
            String key = entry.getKey();
            while( tmp.indexOf( key ) != -1  ) {
                String pre =  tmp.substring(0, tmp.indexOf( key ));
                String post = tmp.substring(tmp.indexOf(key)+ key.length());
                tmp = pre + entry.getValue() + post;
            }
        }

        return tmp;
    }

    public void sendCloseRemind(long aid, long raid){
        ReviewAction ra = callStub.callStub("getReviewActionById", ReviewAction.class, "actionid", raid);
        List<User> u = new LinkedList<>();
        u.add(User.builder().username(ra.getReviewerName()).email(ra.getReviewerEmail()).build());
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        sendMessageAuto(u, infos, "Review Close Notify", null);
    }
    
    public void sendResultNumNotifyMail(long aid, long raid, int num){
        ReviewAction ra = callStub.callStub("getReviewActionById", ReviewAction.class, "actionid", raid);
        User editor = jsHelper.getSectionEditor(aid);
        List<User> u = new LinkedList<>();
        u.add(editor);

        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        infos.put("#Result Num#", num+"");
        sendMessageAuto(u, infos, "Review Result Num Notify", null);
    }

    public void sendDeclineNotifyMail(long aid, long raid){
        ReviewAction ra = callStub.callStub("getReviewActionById", ReviewAction.class, "actionid", raid);
        User editor =  callStub.callStub("findUserById", User.class, "id", ra.getEditorId());
        List<User> u = new LinkedList<>();
        u.add(editor);

        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        infos.put("#Reviewer Name#", ra.getReviewerName());
        sendMessageAuto(u, infos, "Decline Notify", null);
    }

    public void sendReviewMail(long raid, String configPoint){
        ReviewAction ra = callStub.callStub("getReviewActionById", ReviewAction.class, "actionid", raid); 
        Link l = callStub.callStub("getReviewLinkByActionId", Link.class, "raid", raid); 
        long aid = ra.getArticleId();
        HashMap<String, String> infos = iHelper.getVariableMap(aid);

        //在审稿人通知提醒，感谢邮件中要额外这三个参数
        String reivewDue = ra.getReviewDue();
        String responseDate = ra.getResponseDue();
        String linkUrl = "/review"+ l.getMD5();
        infos.put("#Reviewers Name#", ra.getReviewerName());
        infos.put("#Review Due#", reivewDue);
        infos.put("#accessUrl#", linkUrl);

        List<User> u = new LinkedList<>();
        u.add(User.builder().username(ra.getReviewerName()).email(ra.getReviewerEmail()).build());
        sendMessageAuto(u, infos, configPoint, null);
    }


    //发送审稿邀请邮件，有几个参数要替换，1. #Reviewers Name#,#Review Response Date#  #Review Due Date#. #accessUrl#
    public void sendReviewInviteMail(long jid, long aid, String title, String content, JSONArray reviewers, JSONArray files){
        //每个REVIEWER JSON中带有参数
        /**
         *  affiliation: "-"
            email: "2645145487@qq.com"
            name: "zhao hou"
            researchfield: "-"
            responseDue: "2021-09-07"
            reviewDue: "2021-09-10"
         */
        for(int i=0; i<reviewers.size(); i++){
            JSONObject obj = reviewers.getJSONObject(i);
            User u = User.builder().username(obj.getString("name")).email(obj.getString("email")).build();

            HashMap<String, String> model = new HashMap<>();
            model.put("#Review Response Date#", obj.getString("responseDue"));
            model.put("#Review Due Date#", obj.getString("reviewDue"));
            model.put("#Reviewers Name#", obj.getString("name"));
            model.put("#accessUrl#", obj.getString("reviewLink"));

            String t = render(title,  model).replaceAll("<br>|<br />|<br/>|<p>|</p>", "\n");;
            String c =  render(content,  model).replaceAll("<br>|<br />|<br/>|<p>|</p>", "\n");;

            sendMessageAuto( true,  jid, u , t, c , files, "REVIEW_INVITE");
        }
    }

    public long sendDecisionMail(long jid, long aid, int recvType, String decisionType, String title, String content,  JSONArray attachments){
        List<User> recvs = getReceiversInArticleProcess( aid,  recvType);
        long mid = -1;
        for(User u: recvs){
            Message m = mComponent.SendJournalMessage(true, jid, u, decisionType, title, content, mComponent.getFiles(attachments));
            mid = m.getId();
        }
        return mid;       
    }

    public List<User> getReceiversInArticleProcess(long aid, int recvType){
        JSONObject jsonStr = callStub.callStub("getArticleById", JSONObject.class, "aid", aid);
        Article a = JSONObject.toJavaObject(jsonStr, Article.class);

        List<User> rst = new LinkedList<>();
        
        if( recvType == EmailRecvEnum.SUBMITOR.getIndex()){
            rst.add( callStub.callStub("findUserById", User.class, "id", a.getSubmitorId()));
        }else if( recvType == EmailRecvEnum.SUBMITORANDCORRESPOND.getIndex()){
            User u = callStub.callStub("findUserById", User.class, "id", a.getSubmitorId());
            rst.add(u);
            for(ArticleAuthor aa : a.getAuthors()){
                if( aa.isCorresponding() && !aa.getEmail().equals(u.getEmail())){
                    rst.add( User.builder().username(aa.getName()).email(aa.getEmail()).build());
                }
            }
        }else if( recvType == EmailRecvEnum.ALLAUTHORS.getIndex()){
            for(ArticleAuthor aa : a.getAuthors()){
                rst.add( User.builder().username(aa.getName()).email(aa.getEmail()).build());
            }
        }
        return rst;
    }

    public long sendMessageAuto( //以期刊名义发给个人，CONFIGPOINT只是在MESSAGE中记录
        boolean isEmail, 
        long jid, 
        User recvs , 
        String title, 
        String content, 
        JSONArray attachments,
        String configPoint
    ){
        
        Message m = null;

        m = mComponent.SendJournalMessage(
            isEmail, 
            jid, recvs, configPoint, title, content, mComponent.getFiles(attachments));
        return m.getId();
    }


    public long sendMessageAuto(List<User> recvs , HashMap<String, String> info, String configPoint, JSONArray attachments){
        JSONObject isEmail = new JSONObject();
        long jid = Long.valueOf(info.get("#jid#"));
        long aid = Long.valueOf(info.get("#Article Id#"));
        String i18n = info.get("i18n");

        EmailTpl tpl = mTplComponent.getDefaultTpl(jid, configPoint, isEmail);
        String title = tpl.renderTitle(info, i18n.equals(I18N.CN));
        String content = tpl.render(info, i18n.equals(I18N.CN));
        Message m = null;

        if( recvs == null ){//如果没有指定收件人，从模板中获取
            int recv = Integer.parseInt( tpl.getRecipient() );
            recvs = getReceiversInArticleProcess( aid, recv);
        }
        for(User u : recvs){
            m = mComponent.SendJournalMessage(
                isEmail.getString("isEmail")!=null, 
                jid, u, configPoint, title, content, mComponent.getFiles(attachments));
        }
        return m.getId();
    }

}
