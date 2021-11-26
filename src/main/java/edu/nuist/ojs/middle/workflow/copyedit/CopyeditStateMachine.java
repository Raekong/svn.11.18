package edu.nuist.ojs.middle.workflow.copyedit;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;
import edu.nuist.ojs.middle.workflow.JournalUserRoleHelpler;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;

@Component
public class CopyeditStateMachine {
    @Autowired
    private JournalUserRoleHelpler jrHelper; 

    @Autowired
    private HistoryHelper hHelper; 

    @Autowired
    private WorkflowMailHelper mailHelper;


    @Autowired
    private ArticleFileHelper aFileHelper;

    @Autowired
    private ArticleUserRoleHelper rHelper;

    @Autowired
    private ArticleInfoHelper iHelper;

    @Autowired
    private CallStub callStub;

    public static final long Copy_Editor_ROLE_ID = 9;
    
    public static final String[] status = {
        "copyedit-waiting-copyedit",
        "copyedit-waiting-revision",
        "copyedit-published"
    };

    public void pass(long aid, long uid, int rid){ 
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        //拿到操作者
        User oper = jrHelper.getEditor(uid); 
        infos.put("opername", oper.getUsername());
        ArticleHistory ah = hHelper.getHistory("COPYEDIT", "copyedit-published", infos, oper, -1);
        ah.setRound( rid ); //从切换到下一轮
        ah = hHelper.save(ah);

        //monitor: 更新MONITOR
        callStub.callStub("monitor", 
                String.class, 
                "data",
                MonitorDataAssembly.assembly(
                    "aid", aid,
                    "endpoint","copyedit-end",
                    "pid", oper.getPublisherId(),
                    "eid", oper.getUserId(), 
                    "eemail", oper.getEmail(), 
                    "ename", oper.getUsername()
            )
        );
    }

    
    public void newRound( long aid, int rid, long uid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        long jid = Long.valueOf( infos.get("#jid#"));
        //拿到操作者
        User oper = jrHelper.getEditor(uid); 
        infos.put("opername", oper.getUsername());
        
        ArticleHistory ah = hHelper.getHistory("COPYEDIT", "copyedit-waiting-copyedit", infos, oper, -1);
        ah.setRound( rid+1 ); //从切换到下一轮
        ah = hHelper.save(ah);
        
    }

    public void revision(long mid, int rid, long uid, long aid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        User editor = jrHelper.getEditor(uid);
        infos.put("opername", editor.getUsername());
        ArticleHistory ah = hHelper.getHistory("COPYEDIT", "copyedit-waiting-revision", infos, editor, mid);
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

    public void execCopyedit(String state, HashMap<String, JournalSetting> jss, HashMap<String, String> info){
        long jid = Long.valueOf( info.get("#jid#"));
        long aid = Long.valueOf( info.get("#Article Id#"));
        User manager = jrHelper.getJournalManager(jid);
        User messageRecv = jrHelper.getDefaultStageEditor(jid, Copy_Editor_ROLE_ID);

        if( messageRecv == null ) {
            messageRecv = manager;
        }
        rHelper.saveEditor(aid,  Copy_Editor_ROLE_ID, messageRecv.getUserId(), true);//更新BOARD,没有决定权
        
        List<User> recvs = new LinkedList<>(); recvs.add(messageRecv);  
        long mid = mailHelper.sendMessageAuto(recvs, info, "Copyedit Notify", null);
        info.put("opername", messageRecv.getUsername());
        ArticleHistory ah = hHelper.getHistory("COPYEDIT", state, info, messageRecv, mid); 
        ah = hHelper.save(ah);
        
        aFileHelper.serizalFileFromLastStatus(ah);

        //monitor: 更新MONITOR
        callStub.callStub("monitor", 
                String.class, 
                "data",
                MonitorDataAssembly.assembly(
                    "endpoint","copyedit", 
                    "aid", aid,
                    "pid", messageRecv.getPublisherId(),
                    "eid", messageRecv.getUserId(), 
                    "eemail", messageRecv.getEmail(), 
                    "ename", messageRecv.getUsername()
            )
        );
    }
}
