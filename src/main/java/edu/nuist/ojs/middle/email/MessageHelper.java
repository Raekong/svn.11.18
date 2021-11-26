package edu.nuist.ojs.middle.email;


import java.util.List;

import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.EmailFile;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class MessageHelper {
    @Autowired
    private CallStub callStub;


    //消息分三类
    //1. 出版社名义发的信，一般是用户注册，重置密码发的信
    //2. 期刊名义发的信，一般是审稿过程中编辑发的信，以期刊的名义
    //3. 用户之间的发的信息
    //所以要分成三类来发
    public Message getPublisherMessage(
        boolean isEmail,
        long pid,
        User recevier,
        String configPoint,
        String title,
        String content,
        List<EmailFile> attachments
    ){
        EmailConfig c = getEmailConfig( pid, true );
        Message m = fillEmailServerParamters(c);
        
        m.setSendId(pid);
        m.setType( Message.PUBLISHER );

        m.setRecvId(recevier.getUserId());
        m.setRecvName(recevier.getUsername());
        m.setRecvEmail(recevier.getEmail());
        
        m.setConfigPoint(configPoint);
        m.setTimestamp( System.currentTimeMillis());
        m.setEmail(isEmail);
        
        m.setTitle(title); 
        m.setContent(content);

        if( attachments!=null && attachments.size() > 0)
            m.setAppendsJSONStr( JSONObject.toJSONString(attachments) );
        return m;
    }

    public Message getUserMessage(
        boolean isEmail,
        User u,
        User recevier,
        String configPoint,
        String title,
        String content,
        List<EmailFile> attachments
    ){
        EmailConfig c = getEmailConfig( u.getPublisherId(), true );
        Message m = fillEmailServerParamters(c);
        
        m.setSendId(u.getUserId());
        m.setSenderName(u.getUsername());
        m.setType( Message.JOURNAL );

        m.setRecvId(recevier.getUserId());
        m.setRecvName(recevier.getUsername());
        m.setRecvEmail(recevier.getEmail());
        
        m.setConfigPoint(configPoint);
        m.setTimestamp( System.currentTimeMillis());
        m.setEmail(isEmail);
        
        m.setTitle(title); 
        m.setContent(content);
        if( attachments!=null && attachments.size() > 0)
            m.setAppendsJSONStr( JSONObject.toJSONString(attachments) );
        return m;
    }

    public Message getJournalMessage(
        boolean isEmail,
        long jid,
        User recevier,
        String configPoint,
        String title,
        String content,
        List<EmailFile> attachments
    ){
        EmailConfig c = getDefaultEmailConfigForJournal( jid );
        Message m = fillEmailServerParamters(c);
        
        m.setSendId(jid);
        m.setType( Message.JOURNAL );

        m.setRecvId(recevier.getUserId());
        m.setRecvName(recevier.getUsername());
        m.setRecvEmail(recevier.getEmail());
        
        m.setConfigPoint(configPoint);
        m.setTimestamp( System.currentTimeMillis());
        m.setEmail(isEmail);
        
        m.setTitle(title); 
        m.setContent(content);
        if( attachments!=null && attachments.size() > 0)
            m.setAppendsJSONStr( JSONObject.toJSONString(attachments) );
        return m;
    }



    public Message fillEmailServerParamters(EmailConfig c){
        Message m = Message.builder()
            .host(c.getHost())
            .port(c.getPort())
            .password(c.getPassword())
            .senderAccount(c.getAccount())
            .senderName(c.getSenderName())
            .build();
        return m;
    }

    public EmailConfig getEmailConfig( long id, boolean isPublish ){
        JSONObject obj = callStub.callStub("getEmailConfig", JSONObject.class, "isPublish", isPublish, "id", id);
        return JSONObject.toJavaObject(obj, EmailConfig.class);

    }

    public EmailConfig getDefaultEmailConfigForJournal( long id ){
        JSONObject obj = callStub.callStub("getDefaultEmailConfigForJournal", JSONObject.class, "id", id);
        return JSONObject.toJavaObject(obj, EmailConfig.class);
    }
}
