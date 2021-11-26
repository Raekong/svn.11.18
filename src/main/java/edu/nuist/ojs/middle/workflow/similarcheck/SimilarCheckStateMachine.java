package edu.nuist.ojs.middle.workflow.similarcheck;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.SimilarCheck;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.resourcemapper.journal.JorunalFileType;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;
import edu.nuist.ojs.middle.workflow.JournalUserRoleHelpler;
import edu.nuist.ojs.middle.workflow.WorkFlowMainStateMachine;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;

@Component
public class SimilarCheckStateMachine {
    @Autowired
    private WorkFlowMainStateMachine flowMachine; 

    @Autowired
    private JournalUserRoleHelpler jrHelper; 

    @Autowired
    private HistoryHelper hHelper; 

    @Autowired
    private WorkflowMailHelper mailHelper;
    
    @Autowired
    private ArticleUserRoleHelper rHelper;

    @Autowired
    private ArticleFileHelper aFileHelper;

    @Autowired
    private SimilarCheckTabHelper tabHelper;

    @Autowired
    private SimilarCheckUploader scUploader;
    
    @Autowired
    private ArticleInfoHelper infoHelper;

    @Autowired
    private JournalSettingHelper jshHelper;  

    @Autowired
    private CallStub callStub;  


    public static final String STAGE = "SIMILARCHECK";   
    public static final long SIMILARITY_EDITOR_ROLE_ID = 10;
    public static final long PREREVIEWER_ROLE_ID = 11;

    public static final String[] status = {
        "similarity-check-prereview",
        "similarity-check-prereview-declined",
        "similarity-check-round",
        "similarity-check-round-failed",
        "similarity-check-round-revision",
        "similarity-check-round-passed",
        "similarity-check-round-declined",
        "similarity-check-round-revisionupload",
    };

    public void previewdecline(long mid, int rid, long uid, long aid){
        HashMap<String, String> infos = infoHelper.getVariableMap(aid);
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory(
            "SIMILARCHECK", 
            "similarity-check-prereview-declined", 
            infos, 
            editor, 
            mid
        );
        ah.setRound(rid);
        ah = hHelper.save(ah);
    }

    public void decline(long mid, int rid, long uid, long aid){
        HashMap<String, String> infos = infoHelper.getVariableMap(aid);
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory(
            "SIMILARCHECK", 
            "similarity-check-round-declined", 
            infos, 
            editor, 
            mid
        );
        ah.setRound(rid);
        ah = hHelper.save(ah);
    }

    public HashMap<String, JournalSetting> prepare(long jid){
        List<JournalSetting> settings = jshHelper.getAllSettingForJounral(jid);
        HashMap<String, JournalSetting> rst = new HashMap<>();
        for(JournalSetting js: settings){
            rst.put(js.getConfigPoint(), js);
        }
        return rst;
    }

    public void sendToSimilarCheck(long jid, long aid, int rid, long uid){
        HashMap<String, String> infos = infoHelper.getVariableMap(aid);
        execSimilarCheck("similarity-check-round",  prepare(jid), infos, 0);
    }

    public void revisionUpload(long jid, long aid, int rid, String fileType, String originName, String innerId){
        HashMap<String, String> infos = infoHelper.getVariableMap(aid);
        //进入下一个阶段
        execSimilarCheck(
            "similarity-check-round", 
            prepare(jid), 
            infos, 
            rid+1
        );

        //拿到要更新的FILE,更新好
        ArticleFile af = aFileHelper.findByAidAndVersionAndFileType(aid, "SIMILARCHECK-"+(rid+1), fileType);
        af.setInnerId(innerId);
        af.setOriginName(originName);
        aFileHelper.saveFile(af);

        ArticleHistory ah = hHelper.getHistory(
            "SIMILARCHECK", 
            "similarity-check-round-revisionupload", 
            infos, 
            User.builder().username("System").build(), 
            0
        );

        ah.setRound(rid+1);
        ah = hHelper.save(ah);
    }   

    public void revision(long mid, int rid, long uid, long aid){
        HashMap<String, String> infos = infoHelper.getVariableMap(aid);
        User editor = jrHelper.getEditor(uid);
        ArticleHistory ah = hHelper.getHistory(
            "SIMILARCHECK", 
            "similarity-check-round-revision", 
            infos, 
            editor, 
            mid
        );
        ah.setRound(rid);
        ah = hHelper.save(ah);
    }

    public static int getStatusIndex( String s ){
        for(int index = 0; index < status.length ; index++){ 
            if(s.equals(status[index])){
                return index;
            }
        }
        return -1;
    }

    //查重发生错误
    public void checkError( SimilarCheck sc ){
       
    }

    public void passCheck( SimilarCheck sc ){
        HashMap<String, String> infos = infoHelper.getVariableMap(sc.getAid());
        sc.setPass(true);

        List<User> editors = jrHelper.getStageEditors(sc.getJid(), SIMILARITY_EDITOR_ROLE_ID );
        if( editors == null ){
            editors = new LinkedList<>();
            editors.add( jrHelper.getJournalManager(sc.getJid()));
        }
        //给编辑发信
        infos.put("#Recevier Name#", editors.get(0).getUsername());
        long mid = mailHelper.sendMessageAuto(editors, infos, "Similar Checked Passed", null);
        ArticleHistory ah = hHelper.getHistory(
            "SIMILARCHECK", 
            "similarity-check-round-passed", 
            infos, 
            User.builder().username("System").build(), 
            mid
        );
        ah.setRound(sc.getRound());
        ah = hHelper.save(ah);

        sc.setPass(true);
        tabHelper.updateSimilarCheck(sc);

        flowMachine.nextStage(STAGE, sc.getJid(), sc.getAid());
    }

    public void unPassCheck( SimilarCheck sc ){
        HashMap<String, String> infos = infoHelper.getVariableMap(sc.getAid());
        sc.setPass(false);
        //给编辑发信
        List<User> editors = jrHelper.getStageEditors(sc.getJid(), SIMILARITY_EDITOR_ROLE_ID );
        if( editors == null ){
            editors = new LinkedList<>();
            editors.add( jrHelper.getJournalManager(sc.getJid()));
        }
        infos.put("#Recevier Name#", editors.get(0).getUsername());
        long mid = mailHelper.sendMessageAuto(editors, infos, "Similar Checked Failed", null);
        ArticleHistory ah = hHelper.getHistory(
            "SIMILARCHECK", 
            "similarity-check-round-failed", 
            infos, 
            User.builder().username("System").build(), 
            mid
        );
        ah.setRound(sc.getRound());
        ah = hHelper.save(ah);
    }

    public void exec( HashMap<String, JournalSetting> jss, HashMap<String, String> infos, String state){ 
        switch(state){
            case "similarity-check-prereview":
                execPreview( state,  jss, infos);
                break;
            case "similarity-check-round":
                execSimilarCheck(state,  jss, infos, 0);
                break;
        }
    }

    public void execPreview(String state, HashMap<String, JournalSetting> jss, HashMap<String, String> info){
        //拿负责预审的人员
        //发信
        //生成HISTORY
        //更改论文处理团队人员组成
        long jid = Long.valueOf( info.get("#jid#"));
        long aid = Long.valueOf( info.get("#Article Id#"));

        User u = jrHelper.getDefaultStageEditor(jid, PREREVIEWER_ROLE_ID);
        
        List<User> recvs = new LinkedList<>(); recvs.add(u);
        long mid = mailHelper.sendMessageAuto(recvs, info, "PreReview Require", null);
        info.put("opername", u.getUsername());//给出HISTORY信息操作人名字

        ArticleHistory ah = hHelper.getHistory("SIMILARCHECK", state, info, User.builder().username("System").build(), mid);
        ah = hHelper.save(ah);
        aFileHelper.serizalFileFromLastStatus(ah);

        rHelper.saveEditor(aid, PREREVIEWER_ROLE_ID, u.getUserId(), true);//更新BOARD,PRECHECKOR拥有决定权

        //monitor: 更新MONITOR    private long eid;  private String eemail; private String ename;
        callStub.callStub("monitor", 
                String.class, 
                "data",
                MonitorDataAssembly.assembly(
                    "endpoint","similarcheck-preview", 
                    "aid", aid,
                    "eid", u.getUserId(), 
                    "eemail", u.getEmail(), 
                    "ename", u.getUsername(), 
                    "sindex", 9
                )
        );
    }

    public void execSimilarCheck(String state, HashMap<String, JournalSetting> jss, HashMap<String, String> info, int round){
        //拿负责查重的人员
        //发信
        //生成HISTORY
        //更改论文处理团队人员组成
        //自动查重
        long jid = Long.valueOf( info.get("#jid#"));
        long aid = Long.valueOf( info.get("#Article Id#"));
        User u = jrHelper.getDefaultStageEditor( jid, SIMILARITY_EDITOR_ROLE_ID);//没有则拿管理员

        List<User> recvs = new LinkedList<>(); recvs.add(u);
        long mid = mailHelper.sendMessageAuto(recvs, info, "Submission Editor Notify", null);
        info.put("opername", u.getUsername());//给出HISTORY信息操作人名字
        ArticleHistory ah = hHelper.getHistory("SIMILARCHECK", state, info, User.builder().username("System").build(), mid);
        ah.setRound(round);
        ah = hHelper.save(ah);
        
        //从上一个状态中复制过来所有的文件
        aFileHelper.serizalFileFromLastStatus(ah);
        //只有第一轮才需要更新
        if( round == 0 )
            rHelper.saveEditor(aid, SIMILARITY_EDITOR_ROLE_ID, u.getUserId(), true);//更新BOARD,论文查重人员有处理权
        //自动上传去查重
        ArticleFile  af = aFileHelper.getSimilarCheckFile(aid, round);
        if(af != null){
            SimilarCheck sc = tabHelper.saveSimilarCheck(jid, af,  round);
            scUploader.execUploader(sc);
            sc.setUploaded(true);
            tabHelper.updateSimilarCheck(sc);
        } 
        //monitor: 更新MONITOR    private long eid;  private String eemail; private String ename;
        callStub.callStub("monitor", 
            String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint","similarcheck", 
                "aid", aid,
                "totalrun", round+1,
                "pid", u.getPublisherId(),
                "eid", u.getUserId(), 
                "eemail", u.getEmail(), 
                "ename", u.getUsername()
            )
        );

    }   

    

}
