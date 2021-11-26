package edu.nuist.ojs.middle.workflow;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.middle.resourcemapper.article.ArticleStatus;
import edu.nuist.ojs.middle.resourcemapper.article.ArticleStatusMapper;
import edu.nuist.ojs.middle.resourcemapper.i18n.I18N;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class HistoryHelper {
    @Autowired
    private ArticleStatusMapper statusMapper; 
    
    @Autowired
    private CallStub callStub; 

    /**
     * History要求每个文章的STATUS都不能相当，即使是重复迭代的过程，如审稿、查重都不能这重复，要加上轮次
     * 因此，aid, status是主键，可以用来获得唯一的HISTORY记录
     * 
     * HISTORY中应该加一个字段，轮次
     * 决定一个HISTORY的，应该是AID, STATUS, ROUND, 不重复的过程，只有一个ROUND 0
     * 
     * @param aid
     * @param status
     * @return
     */
    public ArticleHistory getLastHistory(long aid, String status, int round){
        return callStub.callStub("getLastHistory", ArticleHistory.class, "aid", aid, "status", status, "round" ,round);
    }

    public ArticleHistory getLastHistoryInRound(long aid, int round){
        return callStub.callStub("getLastHistoryInRound", ArticleHistory.class, "aid", aid, "round", round);
    }

    public ArticleHistory getNextHistory(long aid, String status, int round){
        return callStub.callStub("getNextHistory", ArticleHistory.class, "aid", aid, "status", status, "round" ,round);
    }

    public ArticleHistory getHistory(long aid, String status, int round){
        return callStub.callStub("getHistory", ArticleHistory.class, "aid", aid, "status", status, "round" ,round);
    }

    public ArticleHistory getHistoryById(long ahid){
        return callStub.callStub("getHistoryById", ArticleHistory.class, "ahid", ahid);
    }

    public String getHistoryByAid(long aid){
        return callStub.callStub("getHistoryByAid", String.class, "aid", aid);
    }

    public List<ArticleHistory> getHistoryByAidandFlow(long aid, String workflow){
        String jsonStr = callStub.callStub("getHistoryByAidandFlow", String.class, "aid", aid, "stage", workflow);
        JSONArray arr = JSONArray.parseArray(jsonStr);
        List<ArticleHistory> rst = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            rst.add( JSONObject.toJavaObject(arr.getJSONObject(i), ArticleHistory.class));
        }
        return rst;
    }

    public String getWorkflowHasPreview(long aid){
        return callStub.callStub("getWorkflowHasPreview", String.class, "aid", aid);
    }
   
    
    public ArticleHistory getHistory(String stage, String state, HashMap<String, String> info, User oper, long mid ){ 
        String i18n = info.get("i18n");
        long aid = Long.valueOf(info.get("#Article Id#"));

        ArticleStatus ast = statusMapper.get(state); 
       
        ArticleHistory ah = ArticleHistory.builder()
                .workflow(stage)
                .aid(aid)
                .desc( getDesc(state, ast.getDesc(i18n.equals(I18N.CN)), info) )
                .userId(oper.getUserId())
                .username(oper.getUsername())
                .userEmail(oper.getEmail())
                .msgId(mid)
                .status(state) //存储YML节点，方便后期取用
                .build();
        return ah;
    }

    public ArticleHistory save(ArticleHistory ah){
        String jsonstr = callStub.callStub("historysave", String.class, "ah", ah);
        return JSONObject.toJavaObject(JSONObject.parseObject(jsonstr), ArticleHistory.class);
    }
    

    public String getDesc(String state, String tpl, HashMap<String, String> info){
        String rst = "";
        switch(state){
            case "review-pre-review":
            case "review-round":
            case "review-decision-decline":
            case "similarity-check-prereview":
            case "review-inreview":
            case "review-decision-accept":
            case "review-decision-revision":
            case "similarity-check-round-declined":
            case "similarity-check-prereview-declined":
            case "copyedit-waiting-copyedit":
                rst = MessageFormat.format(tpl, info.get("opername"));
                break;
            case "similarity-check-round":
            case "similarity-check-round-revision":
            case "similarity-check-round-failed":
            case "similarity-check-round-passed":
            case "similarity-check-round-revisionupload":
            case "payment-waiting":
            case "payment-paid":
            case "copyedit-waiting-revision":
            case "copyedit-published":
                rst = tpl;
                break;
            case "review-suggest":
                rst = MessageFormat.format(tpl, info.get("opername"), info.get("suggest"));
                break;
            case "review-decision-changejournal":
                rst = MessageFormat.format(tpl, info.get("journal"), info.get("section"), info.get("ids"));
                break;

        }
        return rst;
    }

   
}
