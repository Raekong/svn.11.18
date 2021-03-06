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
        //?????????????????????
        long jid = Long.valueOf( infos.get("#jid#") );
        User manager = jrHelper.getJournalManager(jid);
        //???????????????????????????,????????????????????????????????????????????????
        User messageRecv = jrHelper.getSectionEditor( aid ); 
        if( messageRecv == null ) {
            messageRecv = manager;
        }else{//??????????????????????????????AEB???
            rHelper.saveEditor(aid, SECTION_EDITOR_ROLE_ID, messageRecv.getUserId(), jrHelper.isSectionAuthority(aid));
        }


        List<User> recvs = new LinkedList<>(); recvs.add(messageRecv);
        long mid = mailHelper.sendMessageAuto(recvs, infos, "Submission Editor Notify", null);
        //todo: ??????????????????????????????
        //??????history
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-round", infos, editor, mid);
        ah = hHelper.save(ah);

        //monitor: ??????REVIEW INFO;
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

    //?????????????????????history, ???????????????????????????????????????????????????????????????????????????????????????
    public void decline(long mid, int rid, long uid, long aid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        
        //??????history
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-decision-decline", infos, editor, mid);
        ah.setRound(rid);
        ah = hHelper.save(ah);

        //monitor: ?????????????????? 
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
        
        //?????????????????????
        if(pnum == null) return;
        else{
             //??????????????????
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
                        
            //????????????      
            JSONObject pay = new JSONObject(); pay.put("pid", p.getId());
            Link l = Link.builder().MD5(md5).api("/payment").jsonData(pay.toJSONString()).build();
            callStub.callStub("savelink", Link.class, "link", l);     
        }

        //monitor: ?????????????????? 
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

        //monitor: ?????????????????? 
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


    //???????????????????????????????????????ID????????????ID,???????????????????????????
    public void changeJournal(long jid, long sid, long aid, long uid, int rid ){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        Journal j = callStub.callStub("journalbyid", Journal.class, "jid", jid);
        JournalSection s = callStub.callStub("getsectionbyid", JournalSection.class, "id", sid);
        
        infos.put("#Section#" , s.getTitle());
        infos.put("#Target JournalTitle#", j.getTitle());

        infos.put("journal", infos.get("#Target JournalTitle#"));
        infos.put("section", infos.get("#Section#"));

        infos.put("ids", Long.valueOf(jid).toString()+","+Long.valueOf(sid).toString());
        //????????????????????????
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
        //?????????,????????????????????????
        infos.put("#Suggest#", suggest);
        infos.put("#EditorName#", editor.getUsername() + "[ "+ editor.getEmail() +" ]");
        List<User> recvs = new LinkedList<>();
        List<JSONObject> rst = rHelper.getBoard(aid);
        for(int i=0; i<rst.size(); i++){
            JSONObject e = rst.get(i);
            //???????????????????????????????????????
            if( e.getString("role").equals("manager") || (e.getString("role").equals("section") && e.getBoolean("decision"))) {
                recvs.add( User.builder().username(e.getString("name")).email(e.getString("email")).build());
            }
        }
        long mid = mailHelper.sendMessageAuto(recvs, infos, "Submission Editor Notify", null);
        //???????????????????????????????????????????????????ROUND???SUGGEST?????????????????????????????????????????????????????????
        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-suggest", infos, editor, 0);
        ah.setRound(rid);
        ah = hHelper.save(ah);        
    }

    public void sendReview(long uid, long aid, int rid ){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getLastHistoryInRound( aid, rid);
        if(ah.getStatus().equals("review-inreview")) return; //???????????????????????????????????????????????????

        ah = hHelper.getHistory("REVIEW", "review-inreview", infos, editor, 0);
        ah.setRound(rid);
        ah = hHelper.save(ah);  
    }

    public void newRound( long aid, int rid, long uid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        long jid = Long.valueOf( infos.get("#jid#"));
        //???????????????
        User oper = jrHelper.getEditor(uid); 
        infos.put("opername", oper.getUsername());
        //??????????????????
        User manager = jrHelper.getJournalManager(jid);
        User messageRecv = jrHelper.getSectionEditor( aid ); //???????????????????????????,????????????????????????????????????????????????
        if( messageRecv == null ) {
            messageRecv = manager;
        }
        //????????????????????????
        /**
         * #Recevier Name#
            #Submitor Name#
         */
        List<User> recvs = new LinkedList<>(); recvs.add(messageRecv);
        infos.put("#Recevier Name#", messageRecv.getUsername());
        infos.put("#Submitor Name#", oper.getUsername());
        long mid = mailHelper.sendMessageAuto(recvs, infos, "Revision Replay", null);

        ArticleHistory ah = hHelper.getHistory("REVIEW", "review-round", infos, oper, mid);
        ah.setRound( rid+1 ); //?????????????????????
        ah = hHelper.save(ah);
        
    }


    public void execReview(String state, HashMap<String, JournalSetting> jss, HashMap<String, String> info){
        //????????????????????????
        //??????
        //??????HISTORY
        //????????????????????????????????????
        long jid = Long.valueOf( info.get("#jid#"));
        long aid = Long.valueOf( info.get("#Article Id#"));
        long submitid = Long.valueOf( info.get("#Submitor id#"));

        User manager = jrHelper.getJournalManager(jid);
        User messageRecv = jrHelper.getSectionEditor( aid ); //???????????????????????????,????????????????????????????????????????????????

        if( messageRecv == null ) {
            messageRecv = manager;
        }else{
            if( messageRecv.getUserId() == submitid && manager.getUserId() != submitid ){
                messageRecv = manager;
            }
        }

        //?????????????????????????????????,?????????????????????,?????????????????????????????????????????????????????????
        //??????????????????
        
        List<User> recvs = new LinkedList<>(); recvs.add(messageRecv);
        long mid = mailHelper.sendMessageAuto(recvs, info, "Submission Editor Notify", null);

        if(messageRecv.getUserId() != manager.getUserId()){//??????????????????????????????
            boolean isAuthority = jrHelper.isSectionAuthority(aid);
            rHelper.saveEditor(aid, SECTION_EDITOR_ROLE_ID, messageRecv.getUserId(), isAuthority);//??????BOARD,???????????????
        }
        
        info.put("opername", messageRecv.getUsername());
        ArticleHistory ah = hHelper.getHistory("REVIEW", state, info, User.builder().username("System").build(), mid);
        ah = hHelper.save(ah);
        //?????????????????????????????????
        aFileHelper.serizalFileFromLastStatus(ah);

        //monitor: ??????REVIEW INFO;
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
        //????????????????????????
        //??????
        //??????HISTORY
        //????????????????????????????????????
        long jid = Long.valueOf( info.get("#jid#"));
        long aid = Long.valueOf( info.get("#Article Id#"));

        User u = jrHelper.getDefaultStageEditor(jid, PREREVIEWER_ROLE_ID);//?????????????????????????????????????????????MANAGER

        List<User> recvs = new LinkedList<>(); recvs.add(u);  
        long mid = mailHelper.sendMessageAuto(recvs, info, "PreReview Require", null);
        info.put("opername", u.getUsername());//??????HISTORY?????????????????????
        ArticleHistory ah = hHelper.getHistory("REVIEW", state, info, User.builder().username("System").build(), mid);//???????????????????????????
        ah = hHelper.save(ah);
        //?????????????????????????????????
        aFileHelper.serizalFileFromLastStatus(ah);
        rHelper.saveEditor(aid, PREREVIEWER_ROLE_ID, u.getUserId(), true);//??????BOARD,PRECHECKOR???????????????

        //monitor: ??????MONITOR    private long eid;  private String eemail; private String ename;
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
