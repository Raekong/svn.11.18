package edu.nuist.ojs.middle.workflow.review;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.Journal;
import edu.nuist.ojs.common.entity.Link;
import edu.nuist.ojs.common.entity.Payment;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.journalsetting.params.JournalSection;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;
import edu.nuist.ojs.middle.workflow.JournalUserRoleHelpler;
import edu.nuist.ojs.middle.workflow.PaymentHelper;
import edu.nuist.ojs.middle.workflow.WorkFlowMainStateMachine;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;

@Component
public class ReviewStateMachine {
    @Autowired
    private JournalUserRoleHelpler jrHelper; 

    @Autowired
    private HistoryHelper hHelper; 

    @Autowired
    private WorkflowMailHelper mailHelper;

    @Autowired
    private CallStub callStub;

    @Autowired
    private ArticleFileHelper aFileHelper;

    @Autowired
    private ArticleUserRoleHelper rHelper;

    @Autowired
    private ArticleInfoHelper iHelper;

    @Autowired
    private PaymentHelper pHelper; 

    @Autowired
    private WorkFlowMainStateMachine stateMachine;

    public static final long PREREVIEWER_ROLE_ID = 11;
    public static final long SECTION_EDITOR_ROLE_ID = 5;

    
    public static final String[] status = {
        "review-pre-review",
        "review-round",
        "review-inreview",
        "review-decision-accept",
        "review-decision-decline",
        "review-decision-revision",
        "review-decision-changejournal"
    };

    public static int getStatusIndex( String s ){
        for(int index = 0; index < status.length ; index++){ 
            if(s.equals(status[index])){
                return index;
            }
        }
        return -1;
    }

    public void exec(HashMap<String, JournalSetting> jss, HashMap<String, String> infos, String state){ 
        switch(state){
            case "review-pre-review":
                execPreview( state,  jss, infos);
                break;
            case "review-round":
                execReview(state,  jss, infos);
                break;
        }
    }

    public void previewPassed(long uid, long aid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        //发信给责任编辑
        long jid = Long.valueOf( infos.get("#jid#") );
        User manager = jrHelper.getJournalManager(jid);
        //默认由责任编辑处理,如果没有配置责任编辑由主管来替代
        User messageRecv = jrHelper.getSectionEditor( aid ); 
        if( messageRecv == null ) {
            messageRecv = manager;
        }else{//把这个责任编辑插入的AEB中
            rHelper.saveEditor(aid, SECTION_EDITOR_ROLE_ID, messageRecv.getUserId(), jrHelper.isSectionAuthority(aid));
        }


        List<User> recvs = new LinkedList<>(); recvs.add(messageRecv);
        long mid = mailHelper.sendMessageAuto(recvs, infos, "Submission Editor Notify", null);
        //todo: 关闭审稿活动，并通知
        //改变history
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-round", infos, editor, mid);
        ah = hHelper.save(ah);

        //monitor: 创建REVIEW INFO;
        callStub.callStub("monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "review",
                "aid", aid,
                "pid", messageRecv.getPublisherId(),
                "eid", messageRecv.getUserId(),
                "eemail", messageRecv.getEmail(),
                "ename", messageRecv.getUsername(),
                "totalrounds", 0,
                "startdate", System.currentTimeMillis(),
                "status", 2
            )
        );
    }

    //被拒稿了，改变history, 发信给作者，查看本轮是否有审稿人，如果有，发送审稿结束通知
    public void decline(long mid, int rid, long uid, long aid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        
        //改变history
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-decision-decline", infos, editor, mid);
        ah.setRound(rid);
        ah = hHelper.save(ah);

        //monitor: 更新本轮结果 
        callStub.callStub(
            "monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "reviewdecision",
                "type", "decline",
                "aid", aid,
                "rid", rid
            )
        );
    }

    public void accept(long mid, int rid, long uid, long aid, long jid, Integer pnum, String md5, String nextStageFileJson){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-decision-accept", infos, editor, mid);
        ah.setRound(rid);
        ah = hHelper.save(ah);
        
        //如果不需要支付
        if(pnum == null) return;
        else{
             //生成支付信息
            JSONObject obj = pHelper.getAPCInfos(jid, pnum);
            Payment p = Payment.builder()
                        .aid(aid)
                        .jid(jid)
                        .orgWire(obj.getIntValue("totalWire"))
                        .orgOnline(obj.getIntValue("totalOnline"))
                        .orgTotalAPC(obj.getIntValue("totalAPC"))
                        .orgPageNumber(pnum)
                        .state(Payment.WAITING)
                        .linkMd5(md5).build();
            p = callStub.callStub("savePayment", Payment.class, "payment", p);     
                        
            //存储外链      
            JSONObject pay = new JSONObject(); pay.put("pid", p.getId());
            Link l = Link.builder().MD5(md5).api("/payment").jsonData(pay.toJSONString()).build();
            callStub.callStub("savelink", Link.class, "link", l);     
        }

        //monitor: 更新本轮结果 
        callStub.callStub(
            "monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "reviewdecision",
                "type", "accept",
                "aid", aid,
                "rid", rid
            )
        );

        stateMachine.reviewAccept(jid, aid, pnum, nextStageFileJson );
    }

    public void revision(long mid, int rid, long uid, long aid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-decision-revision", infos, editor, mid);
        ah.setRound(rid);
        ah = hHelper.save(ah);

        //monitor: 更新本轮结果 
        callStub.callStub(
            "monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "reviewdecision",
                "type", "revision",
                "aid", aid,
                "rid", rid
            )
        );
    }


    //转刊，设定转刊的目标期刊的ID和栏目的ID,等待用户回复与确认
    public void changeJournal(long jid, long sid, long aid, long uid, int rid ){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        Journal j = callStub.callStub("journalbyid", Journal.class, "jid", jid);
        JournalSection s = callStub.callStub("getsectionbyid", JournalSection.class, "id", sid);
        
        infos.put("#Section#" , s.getTitle());
        infos.put("#Target JournalTitle#", j.getTitle());

        infos.put("journal", infos.get("#Target JournalTitle#"));
        infos.put("section", infos.get("#Section#"));

        infos.put("ids", Long.valueOf(jid).toString()+","+Long.valueOf(sid).toString());
        //发送邮件通知作者
        long msgid = mailHelper.sendMessageAuto(null, infos, "Change Journal", null);

        User editor = jrHelper.getEditor(uid);
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-decision-changejournal", infos, editor, msgid);
        ah.setRound(rid);
        hHelper.save(ah);
    }

    public void suggest(int rid, long uid, long aid, String suggest){
        User editor = jrHelper.getEditor(uid);
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        infos.put("opername", editor.getUsername());
        infos.put("suggest", suggest);
        //发信息,准备内容与接收者
        infos.put("#Suggest#", suggest);
        infos.put("#EditorName#", editor.getUsername() + "[ "+ editor.getEmail() +" ]");
        List<User> recvs = new LinkedList<>();
        List<JSONObject> rst = rHelper.getBoard(aid);
        for(int i=0; i<rst.size(); i++){
            JSONObject e = rst.get(i);
            //收集主管与有权限的责任编辑
            if( e.getString("role").equals("manager") || (e.getString("role").equals("section") && e.getBoolean("decision"))) {
                recvs.add( User.builder().username(e.getString("name")).email(e.getString("email")).build());
            }
        }
        long mid = mailHelper.sendMessageAuto(recvs, infos, "Submission Editor Notify", null);
        //新状态，建议状态，但是这个状态只对ROUND的SUGGEST起作用，不放入到正式状态，只是过渡状态
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-suggest", infos, editor, 0);
        ah.setRound(rid);
        ah = hHelper.save(ah);        
    }

    public void sendReview(long uid, long aid, int rid ){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getLastHistoryInRound( aid, rid);
        if(ah.getStatus().equals("review-inreview")) return; //如果已经是在审稿状态下，就不更新了

        ah = hHelper.getHistory("REVIEW", "review-inreview", infos, editor, 0);
        ah.setRound(rid);
        ah = hHelper.save(ah);  
    }

    public void newRound( long aid, int rid, long uid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        long jid = Long.valueOf( infos.get("#jid#"));
        //拿到操作者
        User oper = jrHelper.getEditor(uid); 
        infos.put("opername", oper.getUsername());
        //拿到责任编辑
        User manager = jrHelper.getJournalManager(jid);
        User messageRecv = jrHelper.getSectionEditor( aid ); //默认由责任编辑处理,如果没有配置责任编辑由主管来替代
        if( messageRecv == null ) {
            messageRecv = manager;
        }
        //发信通知责任编辑
        /**
         * #Recevier Name#
            #Submitor Name#
         */
        List<User> recvs = new LinkedList<>(); recvs.add(messageRecv);
        infos.put("#Recevier Name#", messageRecv.getUsername());
        infos.put("#Submitor Name#", oper.getUsername());
        long mid = mailHelper.sendMessageAuto(recvs, infos, "Revision Replay", null);

        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-round", infos, oper, mid);
        ah.setRound( rid+1 ); //从切换到下一轮
        ah = hHelper.save(ah);
        
    }


    public void execReview(String state, HashMap<String, JournalSetting> jss, HashMap<String, String> info){
        //拿负责编辑的人员
        //发信
        //生成HISTORY
        //更改论文处理团队人员组成
        long jid = Long.valueOf( info.get("#jid#"));
        long aid = Long.valueOf( info.get("#Article Id#"));
        long submitid = Long.valueOf( info.get("#Submitor id#"));

        User manager = jrHelper.getJournalManager(jid);
        User messageRecv = jrHelper.getSectionEditor( aid ); //默认由责任编辑处理,如果没有配置责任编辑由主管来替代

        if( messageRecv == null ) {
            messageRecv = manager;
        }else{
            if( messageRecv.getUserId() == submitid && manager.getUserId() != submitid ){
                messageRecv = manager;
            }
        }

        //投稿人就是责任编辑本人,但不是期刊主管,此时应通知主管处理，而不让责任编辑处理
        //责任编辑回避
        
        List<User> recvs = new LinkedList<>(); recvs.add(messageRecv);
        long mid = mailHelper.sendMessageAuto(recvs, info, "Submission Editor Notify", null);

        if(messageRecv.getUserId() != manager.getUserId()){//如果处理人员不是主管
            boolean isAuthority = jrHelper.isSectionAuthority(aid);
            rHelper.saveEditor(aid, SECTION_EDITOR_ROLE_ID, messageRecv.getUserId(), isAuthority);//更新BOARD,没有决定权
        }
        
        info.put("opername", messageRecv.getUsername());
        ArticleHistory ah = hHelper.getHistory("REVIEW", state, info, User.builder().username("System").build(), mid);
        ah = hHelper.save(ah);
        //从上一轮文件拷贝到本轮
        aFileHelper.serizalFileFromLastStatus(ah);

        //monitor: 创建REVIEW INFO;
        callStub.callStub("monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "review",
                "aid", aid,
                "pid", messageRecv.getPublisherId(),
                "eid", messageRecv.getUserId(),
                "eemail", messageRecv.getEmail(),
                "ename", messageRecv.getUsername(),
                "totalrounds", 0,
                "startdate", System.currentTimeMillis(),
                "status", 2
            )
        );
    }

    public void execPreview(String state, HashMap<String, JournalSetting> jss, HashMap<String, String> info){
        //拿负责预审的人员
        //发信
        //生成HISTORY
        //更改论文处理团队人员组成
        long jid = Long.valueOf( info.get("#jid#"));
        long aid = Long.valueOf( info.get("#Article Id#"));

        User u = jrHelper.getDefaultStageEditor(jid, PREREVIEWER_ROLE_ID);//拿负责预审的编辑，如果没有就拿MANAGER

        List<User> recvs = new LinkedList<>(); recvs.add(u);  
        long mid = mailHelper.sendMessageAuto(recvs, info, "PreReview Require", null);
        info.put("opername", u.getUsername());//给出HISTORY信息操作人名字
        ArticleHistory ah = hHelper.getHistory("REVIEW", state, info, User.builder().username("System").build(), mid);//操作人员设置成系统
        ah = hHelper.save(ah);
        //从上一轮文件拷贝到本轮
        aFileHelper.serizalFileFromLastStatus(ah);
        rHelper.saveEditor(aid, PREREVIEWER_ROLE_ID, u.getUserId(), true);//更新BOARD,PRECHECKOR拥有决定权

        //monitor: 更新MONITOR    private long eid;  private String eemail; private String ename;
        callStub.callStub("monitor", 
                String.class, 
                "data",
                MonitorDataAssembly.assembly(
                    "endpoint","review-preview", 
                    "aid", aid,
                    "eid", u.getUserId(), 
                    "eemail", u.getEmail(), 
                    "ename", u.getUsername(), 
                    "sindex", 1
            )
        );
    }

}
