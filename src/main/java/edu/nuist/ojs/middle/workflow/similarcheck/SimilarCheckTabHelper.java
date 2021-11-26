package edu.nuist.ojs.middle.workflow.similarcheck;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import edu.nuist.ojs.common.entity.SimilarCheck;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;

@Component
public class SimilarCheckTabHelper {

    @Autowired
    private CallStub callStub;

    @Autowired
    private MessageComponent mComponent;

    @Autowired
    private JournalSettingHelper jHelper;

    @Autowired
    private HistoryHelper helper;

    @Autowired
    private ArticleFileHelper aFileHelper; 

  
    public boolean isPassed( SimilarCheck sc ){
        int[] levels = jHelper.getSimilarCheckSetting(sc.getJid());
        if( levels == null ){ //不用查重
            return true;
        }
        
        for(int level : levels){
            System.out.println( level );
        }
        
        if( Double.valueOf( sc.getTotalSimilar().replaceAll("[^0-9.]", "")) > levels[0] ) return false;
        if( Double.valueOf( sc.getFrsSimilar().replaceAll("[^0-9.]", "")) > levels[1] ) return false;
        if( Double.valueOf( sc.getSecSimilar().replaceAll("[^0-9.]", "")) > levels[1] ) return false;
        if( Double.valueOf( sc.getThrSimilar().replaceAll("[^0-9.]", "")) > levels[1] ) return false;
        return true;
    }

    public void analysis(SimilarCheckRound rr, List<ArticleHistory> histories, boolean isZH){
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
        rr.setHasPreview(false);

        int statusIndex = SimilarCheckStateMachine.getStatusIndex(current.getStatus());
        rr.setCurstate(statusIndex);

        int round = current.getRound();
        long aid = current.getAid();
        SimilarCheck sc = callStub.callStub("findSimilarCheckByAidAndRound", SimilarCheck.class, "aid", aid, "round", round);
        rr.setSc(sc);
        
        //处理PREVIEW
        if( statusIndex == 0 || statusIndex == 1  ){//预审态或预审被终止态
            List<ArticleFile> afs = aFileHelper.getFiles(current.getId());
            List<JSONObject> files = new LinkedList<>(); 
            for( ArticleFile af : afs){
                JSONObject obj =  (JSONObject)JSONObject.toJSON(af);
                obj.put("link", mComponent.getFileUrl(af.getInnerId()));
                files.add( obj );
            }
            rr.setPreviewFiles( files);
        }
    
    }

    //拿到HISTORY，并转换成REVIEWROUN列表,并返回
    public List<SimilarCheckRound> getRounds( long aid, boolean isZH ){
        //返回按ID倒序排的HISTORY列表
        List<ArticleHistory> histories = helper.getHistoryByAidandFlow(aid, "SIMILARCHECK");
        //通过ROUND字段来区分不同的ROUND
        List<SimilarCheckRound> rounds = new LinkedList<>(); 
        
        int currentRound = -1;
        SimilarCheckRound rr = null;
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
                rr = new SimilarCheckRound();
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

    public SimilarCheck saveSimilarCheck(long jid, ArticleFile af, int round){
        SimilarCheck sc = SimilarCheck.builder() 
                        .jid(jid)
                        .aid(af.getAid()) 
                        .title(af.getInnerId())
                        .fileName(af.getOriginName())
                        .fileType(af.getFileType())
                        .round(round)
                        .link(mComponent.getFileUrl(af.getInnerId())).build();
        return callStub.callStub("saveSimilarCheck", SimilarCheck.class, "similarCheck", sc);
    }


   public SimilarCheck updateSimilarCheck(SimilarCheck sc){
        return callStub.callStub("saveSimilarCheck", SimilarCheck.class, "similarCheck", sc);
    }

    
}
