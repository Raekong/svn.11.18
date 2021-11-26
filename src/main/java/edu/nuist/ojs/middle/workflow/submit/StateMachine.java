package edu.nuist.ojs.middle.workflow.submit;

import java.text.MessageFormat;
import java.util.HashMap;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.middle.resourcemapper.article.ArticleStatus;
import edu.nuist.ojs.middle.resourcemapper.article.ArticleStatusMapper;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.WorkFlowMainStateMachine;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;

/**
 * submit 状态机
 * 投稿时使用
 * 1. 投稿完成后，决定下一步，有二种，一种是存在预审，一种是不存在
 * 2. 存在预审，进入预审状态，发送消息给预审负责人员
 * 3. 不存在，直接进入下一个流程，本流程结束，要调用全局WORKFLOW来设置论文状态
 */
@Component
public class StateMachine {
    public static final String STAGE = "SUBMIT";   
    
    @Autowired
    private WorkFlowMainStateMachine globalMachine;

    @Autowired
    private CallStub callStub;

    @Autowired
    private ArticleStatusMapper statusMapper; 

    @Autowired
    private WorkflowMailHelper mailHelper; 

    @Autowired
    private ArticleInfoHelper aInfoHelper;


    /**
     * 流程的说明，首先不管怎么样，都要发送一封确认邮件给作者（根据配置可能还会发给其它人）
     * 然后直接进入下一个阶段，下一个阶段由GLOBALSTATUSMACHINE来负责了
     */

    public static final long JOURNAL_MANAGER_ROLEID = 1l;

    public void exec( long pid, long jid, long aid, long uid, String i18n){

        HashMap<String, String> info = aInfoHelper.getVariableMap(aid);
        //发送审稿确认邮件
		long msgid = mailHelper.sendMessageAuto(null, info, JournalConfigPointEnum.SUBMISSION_ACK, null);
        
        ArticleStatus ast = statusMapper.get("submit");
        String ahDescTxt = "[" + aid + "] "+ info.get("#Article Title#");
        ArticleHistory ah = ArticleHistory.builder()
                .workflow(STAGE)
                .aid(aid)
                .desc( MessageFormat.format(ast.getDesc(i18n.equals(I18N.CN)),  ahDescTxt) )
                .userId(uid)
                .username(info.get("#Submitor Name#"))
                .userEmail(info.get("#Submitor Email#"))
                .round(0)
                .msgId(msgid)
                .status("submit") //存储YML节点，方便后期取用
                .build();
        callStub.callStub("historysave", String.class, "ah", ah);
        //submisson的流程到这里就结束了
        globalMachine.nextStage(STAGE, jid, aid);
    }

}
