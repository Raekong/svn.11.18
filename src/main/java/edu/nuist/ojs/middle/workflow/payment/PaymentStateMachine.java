package edu.nuist.ojs.middle.workflow.payment;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;
import edu.nuist.ojs.middle.workflow.JournalUserRoleHelpler;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;

@Component
public class PaymentStateMachine {
    public static final String[] status = {
        "payment-waiting",
        "payment-paid",
        "payment-audit-pass"
    };

    public static final long Financial_EDITOR_ROLE_ID = 7;

    @Autowired
    private HistoryHelper hHelper;

    @Autowired
    private ArticleInfoHelper iHelper;

    @Autowired
    private ArticleFileHelper fHelper;

    @Autowired
    private JournalUserRoleHelpler jrHelper;  

    @Autowired
    private ArticleUserRoleHelper rHelper;

    @Autowired
    private WorkflowMailHelper mailHelper;

    
    @Autowired
    private CallStub callStub; 
    
    //判断一篇文章是否可以支付，
    public boolean canPay(long aid){
        List<ArticleHistory> histories = hHelper.getHistoryByAidandFlow(aid, "PAYMENT"); 
        if(histories.size() == 0)  return false;//没有进入到支付环节
        for(ArticleHistory ah : histories){
            if(ah.getStatus().equals("payment-audit-pass"))//已经PASS了
            {
                return false;
            }        
        }

        return true;
    }

    public void payAudit(long aid, long jid, long uid){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        User u = jrHelper.getEditor(uid);
        ArticleHistory ah = hHelper.getHistory("PAYMENT", "payment-audit-pass", infos, u, -1); 
        ah = hHelper.save(ah);

        callStub.callStub("payaudited", String.class, "aid", aid);
    }

    public void paid(long aid, int total){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);
        User recevierUser = User.builder()
            .email("System")
            .username("System")
            .build();

        long jid = Long.valueOf( infos.get("#jid#") );
        User manager = jrHelper.getJournalManager(jid);
        //默认由财务编辑处理,如果没有配置责任编辑由主管来替代
        User u = jrHelper.getDefaultStageEditor(jid, Financial_EDITOR_ROLE_ID);
        if( u == null ) u = manager;
        List<User> recvs = new LinkedList<>(); recvs.add(u);
        long mid = mailHelper.sendMessageAuto(recvs, infos, "APC Paid Notify", null); 
        //进入新状态
        ArticleHistory ah = hHelper.getHistory("PAYMENT", "payment-paid", infos, recevierUser, mid); 
        ah = hHelper.save(ah);
        callStub.callStub("monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "paid",
                "aid", aid,
                "pid", u.getPublisherId(),
                "total", total
            )
        );

    }

    public void exec( long jid, long aid, int pnum, String nextStageFileJson){
        HashMap<String, String> infos = iHelper.getVariableMap(aid);

        User recevierUser = User.builder()
            .email("System")
            .username("System")
            .build();
        //进入新状态
        ArticleHistory ah = hHelper.getHistory("PAYMENT", "payment-waiting", infos, recevierUser, -1); 
        ah = hHelper.save(ah);

        //更新编辑团队
        User manager = jrHelper.getJournalManager(jid);
        //默认由财务编辑处理,如果没有配置责任编辑由主管来替代
        User u = jrHelper.getDefaultStageEditor(jid, Financial_EDITOR_ROLE_ID);
        if( u == null ) u = manager;
        rHelper.saveEditor(aid, Financial_EDITOR_ROLE_ID, u.getUserId(), true);//更新BOARD,

        //序列化文件
        JSONArray arr = JSONObject.parseArray(nextStageFileJson); 
        List<ArticleFile> files = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            long fid = arr.getLong(i);
            ArticleFile f = fHelper.getArticleFileById(fid);
            f.setVersion("PAYMENT-0");
            files.add(f);
        }
        fHelper.serizalFiles(files);

        //monitor: 创建REVIEW Round;
        callStub.callStub("monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "payment",
                "aid", aid,
                "pid", u.getPublisherId(),
                "eemail", u.getEmail(),
                "ename", u.getUserFullName(),
                "eid", u.getUserId()
            )
        );

    }


}
