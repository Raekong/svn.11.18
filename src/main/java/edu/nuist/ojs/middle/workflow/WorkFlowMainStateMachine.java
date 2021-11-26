package edu.nuist.ojs.middle.workflow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.Article;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.resourcemapper.article.ArticleStatus;
import edu.nuist.ojs.middle.resourcemapper.article.ArticleStatusMapper;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.copyedit.CopyeditStateMachine;
import edu.nuist.ojs.middle.workflow.payment.PaymentStateMachine;
import edu.nuist.ojs.middle.workflow.review.ReviewStateMachine;
import edu.nuist.ojs.middle.workflow.similarcheck.SimilarCheckStateMachine;

@Component
public class WorkFlowMainStateMachine {
    @Autowired
    private CallStub callStub;
    
    @Autowired
    private JournalSettingHelper jshHelper; 

    @Autowired
    private ArticleStatusMapper statusMapper;

    @Autowired
    private SimilarCheckStateMachine scMachine;

    @Autowired
    private  PaymentStateMachine pMachine; 

    @Autowired
    private ReviewStateMachine rMachine;

    @Autowired
    private CopyeditStateMachine cMachine;

    @Autowired
    private ArticleInfoHelper infoHelper;

    @Autowired
    private ArticleUserRoleHelper rHelper;
    
    @Autowired
    private JournalUserRoleHelpler jrHelper;
    public static final long JOURNAL_MANAGER_ROLEID = 1l;

    /**&&&&&&&&&&=======这里是大的阶段
     * 这是进入下一个大的阶段的入口调用，从当前一个阶段进入到下一个新的阶段，由这个方法来判断
     * 具体进入到哪一个阶段
     * 进入阶段要提交的参数：
     * 哪一篇文章？AID
     * 进入到哪一个阶段的一个入口，一个阶段的状态机可能有多个入口也可能有多个出口
     * 这个方法的目标就是根据上一个阶段出口，找到下一个阶段的入口
     * &&&&&&&&&&========然后调用各个大阶段的STATEMACHINE
     * 每一个大阶段中小状态的处理不外乎：
     * 1. 人＝＝>哪些编辑，哪些作者
     * 2. 信息＝＝>文章，出版社，期刊
     * 发送邮件也是，模板，收件人，发件人，以上信息的汇总，
     * 汇总的信息由：INFOHELPER负责，包括国际化的信息，这一部分现在缺国际化
     * 编辑的信息由: USERROLEHELPER负责， 这一部分基本完成
     * 邮件的模板由：MAILHELPER负责， 这一部分现在太烦，要简化
     * 状态历史信息由：这个部分还没有写，这一部分要做
     * 还有一个处理团队管理
     * &&&&&&&&&&============这个当中有些东西交织不清，NEXTSTAGE中有些信息获取完之后，又丢掉，又要重新在子状态获取很烦
     * 用CACHE的话又怕有改变
     */
    
    public void changeJournalDo(long jid, long sid, long aid ){
        callStub.callStub("changeJournal", String.class, "jid", jid, "aid", aid, "sid", sid);
        nextStage("SUBMIT",  jid,  aid) ;
    }

    public void nextStage(String currentStage, long jid, long aid){
        HashMap<String, JournalSetting> settings =  prepare(jid);
        HashMap<String, String> infos =  getInfos( aid );
        switch(currentStage){
            case "SUBMIT": {
                //插入管理员到EDITOR BOARD
                User u = jrHelper.getJournalManager(jid);
                callStub.callStub("articleboardsave", String.class, "aid", aid, "rid",  JOURNAL_MANAGER_ROLEID, "uid", u.getUserId(),  "decision", true);
            
                //要不要先预审
                Boolean needPreCheck = Boolean.valueOf( settings.get("Technical Check") == null? "false" : settings.get("Technical Check").getConfigContent());
                //要不要查重
                Boolean smCheck = Boolean.valueOf( settings.get("Simaliary Check") == null? "false" : settings.get("Simaliary Check").getConfigContent() );
                //要不要先查重
                Boolean smCheckFirst = Boolean.valueOf( settings.get("Simaliary Check First") == null? "false" : settings.get("Simaliary Check First").getConfigContent() );
                
                if( !smCheck || (smCheck && !smCheckFirst)){//不要查重或者不要先查重
                    //进入审稿
                    if( needPreCheck ){//进入审稿预审
                        rMachine.exec(settings, infos,  "review-pre-review");
                    }else{//进入审稿
                        rMachine.exec(settings,infos,   "review-round");
                    }
                }else if(smCheckFirst ){//要查重还要先查重  
                    if( needPreCheck ){//进入查重预审
                        scMachine.exec(settings, infos, "similarity-check-prereview");
                    }else{//进入查重
                        scMachine.exec(settings, infos, "similarity-check-round");
                    }
                }
                break;
            }
            case "SIMILARCHECK": //查重结束了
                //检查下一步，如果是先查重的，则要进入审稿，如果是后查重的，此时已经付过账，则进入排版
                JournalSetting js = settings.get(JournalConfigPointEnum.Simaliary_Check_First);
                if( js != null && js.getConfigContent().equals("true")){//进入审稿
                    //和投稿进入审稿一样的
                    rMachine.exec(settings, infos, "review-round");
                }else{//进入出版
                    cMachine.execCopyedit("copyedit-waiting-copyedit", settings, infos);
                }
                break;
        }
    }

    //REVIEW 接收了
    public void reviewAccept(long jid, long aid, int pnum, String nextStageFileJson){
        HashMap<String, JournalSetting> settings =  prepare(jid);
        HashMap<String, String> infos =  getInfos( aid );
        //从REVIEW出来，只有二种走向，一种是支付，如果没有支付，则查看查重，如果也没有查重或者已经先查重，则会直接到出版
        JournalSetting payment = settings.get("Payment");
        if( payment !=null && payment.getConfigContent().equals("true")){ //如果需要支付，则走向支付
            pMachine.exec( jid,  aid,  pnum,  nextStageFileJson);
        }else{

            //要不要查重
            Boolean smCheck = Boolean.valueOf( settings.get("Simaliary Check") == null? "false" : settings.get("Simaliary Check").getConfigContent() );
            //要不要先查重
            Boolean smCheckFirst = Boolean.valueOf( settings.get("Simaliary Check First") == null? "false" : settings.get("Simaliary Check First").getConfigContent() );
            if( smCheckFirst ){//走向出版
                cMachine.execCopyedit("copyedit-waiting-copyedit", settings, infos);
            }else if( smCheck ){ //后查重
                scMachine.exec(settings, infoHelper.getVariableMap(aid), "similarity-check-round");
            }else{//走向出版
                cMachine.execCopyedit("copyedit-waiting-copyedit", settings,  infos);
            }
        }
    }

    //完成支付
    public void payed(long jid, long aid){
        HashMap<String, JournalSetting> settings =  prepare(jid);
        HashMap<String, String> infos =  getInfos( aid );
        //支付
        //要不要查重
        Boolean smCheck = Boolean.valueOf( settings.get("Simaliary Check") == null? "false" : settings.get("Simaliary Check").getConfigContent() );
        //要不要先查重
        Boolean smCheckFirst = Boolean.valueOf( settings.get("Simaliary Check First") == null? "false" : settings.get("Simaliary Check First").getConfigContent() );
        
        if(smCheckFirst){ //先查重,就不用再查了, 进入出版
            cMachine.execCopyedit("copyedit-waiting-copyedit", settings,  infos);
        }else if(smCheck){//要查重
            scMachine.exec(settings, infoHelper.getVariableMap(aid), "similarity-check-round");
        }else{ //不要查重,进入出版
            cMachine.execCopyedit("copyedit-waiting-copyedit", settings,  infos);
        }   
    }



    //拿到所有的期刊设置
    public HashMap<String, JournalSetting> prepare(long jid){
        List<JournalSetting> settings = jshHelper.getAllSettingForJounral(jid);
        HashMap<String, JournalSetting> rst = new HashMap<>();
        for(JournalSetting js: settings){
            rst.put(js.getConfigPoint(), js);
        }
        return rst;
    }

    //拿到所有的文章信息，包括I18N
    private HashMap<String, String> getInfos(long aid){
        return infoHelper.getVariableMap(aid);
    }

    /**
     * 
     * 这个方法用来提供论文页面头部的消息
     * @return
     */
    public HashMap<String, Object> getPageHeadInfo( long id, HashMap<String, Boolean> roles, boolean isZH){
        HashMap<String, Object> rst = new HashMap<String, Object>();

        String a = callStub.callStub("getArticleById", String.class, "aid", id);
        Article article = Article.getArticle(JSONObject.parseObject(a));
        rst.put("a", article);

        ArticleHistory ah = callStub.callStub("getLastStatusById", ArticleHistory.class, "aid", id);
        ArticleStatus  status = statusMapper.get(ah.getStatus());
        rst.put("status", status.getStatus(isZH));

        //打印用户对这篇文章的ROLE列表
        System.out.println("sssssssssssssss==============sssssssssssssss");
        System.out.println(roles);
        //准备卡片页
        String tabs = callStub.callStub("getTabs", String.class, "aid", id); //根据HISTORY得到卡片页
        JSONArray tabArr = JSONObject.parseArray(tabs);
        tabArr.add("HISTORY"); //HISTORY 卡片页是必须有的

        List<HashMap<String, String>> tmp = new LinkedList<>();
        for(int i=0; i<tabArr.size(); i++){
            String tab = tabArr.getString(i);
            if(! rHelper.getTabsForUser(id, roles, tab)) continue; //拿这个用户对这篇文章的权限roles如果用户没有对应卡片页的权限，则跳过
            
            HashMap<String, String> param = new HashMap<String, String>();
            param.put("tab", statusMapper.getWorkFlow(tabArr.getString(i), isZH)); //从WORK FLOW这边拿的卡片页
            param.put("stage", tabArr.getString(i));
            param.put("url", "article/tab/" + tabArr.getString(i).toLowerCase() + "::tab");
            tmp.add( param );
        } 
        rst.put("tabs", tmp);

        return rst;
    }
}
