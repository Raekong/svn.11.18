package edu.nuist.ojs.middle.workflow.copyedit;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;

@Component
public class CopyeditTabHelper {
    @Autowired
    private HistoryHelper helper;
    
    @Autowired
    private CallStub callStub;

    @Autowired
    private ArticleFileHelper afHelper; 

    public void analysis(CopyeditRound rr, List<ArticleHistory> histories, boolean isZH){
        Collections.sort(histories, new Comparator<ArticleHistory>() {
            @Override
            public int compare(ArticleHistory h1, ArticleHistory h2) {
                long diff = h2.getId() - h1.getId();
                    if (diff > 0) {
                        return 1;
                    }else if (diff < 0) {
                        return -1;
                    }
                    return 0;//相等为0
                }
        }); 

        ArticleHistory current = histories.get(0);

        rr.setLastStats(current.getStatus());
        rr.setDesc(current.getDesc());

        int statusIndex = CopyeditStateMachine.getStatusIndex(current.getStatus());
        rr.setCurstate(statusIndex);
        
        long fileAhId = histories.get(histories.size()-1).getId();
        rr.setFiles(afHelper.getFiles(fileAhId));

        String msgJson = callStub.callStub("getDiscuss", String.class, "aid", rr.getAid(), "rid", rr.getIndex());
        JSONArray arr = JSONArray.parseArray(msgJson);
        List<Message> msgs = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            long mid = arr.getJSONObject(i).getLong("msgId");
            Message m = callStub.callStub("getMessage", Message.class, "id", mid);
            //这里是借用原MESSAGE中TYPE属性，该属性本来是为了区分是期刊消息，还是出版社消息
            //在这里，用作是MANAGER的消息，还是作者讨论的消息
            m.setType(arr.getJSONObject(i).getIntValue("type"));
            msgs.add(m);
        }
        rr.setMsgs(msgs);
    }

    
    //拿到HISTORY，并转换成CopyeditRound列表,并返回
    public List<CopyeditRound> getRounds( long aid, boolean isZH ){
        //返回按ID倒序排的HISTORY列表
        List<ArticleHistory> histories = helper.getHistoryByAidandFlow(aid, "COPYEDIT");
        //通过ROUND字段来区分不同的ROUND
        List<CopyeditRound> rounds = new LinkedList<>();   
        
        int currentRound = -1;
        CopyeditRound rr = null;
        List<ArticleHistory> historiesTmp = new LinkedList<>();
        for(ArticleHistory ah : histories){
            if( ah.getRound() != currentRound ){ //开始新一轮
                if( rr != null ){
                    analysis(rr, historiesTmp, isZH);
                    if(rounds.size()>=1){
                        rr.setClosed(true);
                    }
                    rounds.add(rr);
                }
                rr = new CopyeditRound();
                rr.setAid(ah.getAid());
                rr.setRoundId(ah.getRound());
                rr.setIndex(ah.getRound());
                currentRound = ah.getRound();
                historiesTmp = new LinkedList<>();
                historiesTmp.add( ah );
            }else{
                historiesTmp.add( ah );
            }
        }
        if( rr != null ) analysis(rr, historiesTmp, isZH);
        if(rounds.size()>=1){
            rr.setClosed(true);
        }
        rounds.add(rr);
        
        return rounds;
    }
}
