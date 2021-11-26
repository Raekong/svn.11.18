package edu.nuist.ojs.middle.workflow.review;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.sis.internal.jaxb.gml.Measure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cn.hutool.json.JSON;
import edu.nuist.ojs.common.entity.Link;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.article.Article;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.common.entity.review.ReviewAction;
import edu.nuist.ojs.common.entity.review.ReviewRecommendType;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalConfig;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;

/**
 * 提供REVIEW卡片页的信息
 * 从HISTORY中拿到一篇文章的REVIEW历史记录
 * 从历史记录中可以拿到每个ROUND对应的文件，比较特殊的是第一个
 * 如果有PREREVIEW，而没有进一步的ROUND，则通过PREREVIEW来构建第一个卡片页
 * 如果有ROUND，则PREREVIEW被丢弃，因为已经过审，不需要了
 * 现在的问题是ROUND中的文件，应该怎么定？
 * 第一个ROUND，文件应该来自于SUBMIT，所以SUBMIT的卡片页中文件应该不能修改，而应该复制一份，放到ROUND 0中
 * 
 * 在这里要确定一个问题，通过HISTORY怎么去确定一个ROUND?
 * 1. 先取回所有的REVIEW阶段，然后根据序号去判断
 * 2. review-pre-review 预审稿开始 review-round: 开始 ， review-inreview: 中间， review-decision-decline: 结束
      review-decision-accept: 结束，review-decision-revision: 结束，review-decision-changejournal － 结束
 * 3. 这个就比较好判断了
 * 4. 审稿活动的问题，这个简单，审稿活动有轮次，直接挂上就行，关键是第一个ROUND 0，这个只能去拿ARTICLE REVIEWERS
 * TAB Helper的工作
 * 1. 拿FILE，确定FILE能不能改
 * 2. 提供审稿人信息
 * 3. 确定各种编辑的权限要求
 * 4. 权限要求是打TAG来实现，不同状态下的同一个TAB，要先通过状态来决定显示的组件（包括文件能否修改），然后再根据
 *    RIGHT的大小来实现显示情况
 * 
 * 思路很清晰
 */

@Component
public class ReviewTabHelper {
    @Autowired
    private HistoryHelper helper;

    @Autowired
    private ArticleFileHelper afHelper;

    @Autowired
    private CallStub callStub;

    @Autowired
    private JournalConfig jConfig;

    @Value("${global.linkserver}")
    private String linkServer;
    
    
    public static String[] REVIEW_CONFIG_POINT ={
        "Response Due", "Review Due"
    };

    public static final String REVISION_CONFIG_POINT = "Revision Due";
    public static final String REVIEW_URL = "/review/";

   
    public void saveReviewActionLinks(long aid, JSONArray reviewers){
        for(int i=0; i<reviewers.size(); i++){
            JSONObject obj = reviewers.getJSONObject(i);
            String params = "{\"review\":\""+ obj.getString("email") +"\", \"actionid\":\""+ obj.getLongValue("actionId") +"\"}";
            //LINK 由 审稿人的EMAIL, 加上AID组成， 形式为：/review/{email}/aid
            Link k = Link.builder().api("/review").jsonData( params ).build();
            String linkJsonStr = JSONObject.toJSONString(k);
            callStub.callStub("saveReviewLink", String.class, "link", linkJsonStr);
            //设置这一次审稿的链接
            System.out.println(linkJsonStr);
            obj.put("reviewLink", linkServer + JSONObject.parseObject(linkJsonStr).getString("mD5"));
        }
    }

    public void saveReviewers(long pid, JSONArray reviewers){
        for(int i=0; i<reviewers.size(); i++){
            JSONObject obj = reviewers.getJSONObject(i);
            callStub.callStub("saveReviewers", String.class, "reviewer", obj, "pid", pid);
        }
    }

    /**
     * private long roundId;
	    private long reviewId;
	    private String responseDue;
	    private String reviewDue;
	    private int curstate;
	    private long articleId;
	    private long editorId;
	    private boolean closed;
	    private long lastUpdate;  
     */
    public void saveReviewAction(  long pid,  long aid, long rid, long uid, JSONArray reviewers, JSONArray reviewfiles){
        for(int i=0; i<reviewers.size(); i++){
            JSONObject obj = reviewers.getJSONObject(i);
            long actionId = callStub.callStub("saveReviewActions", long.class, "reviewer", obj, "files", reviewfiles.toJSONString(), "pid", pid, "aid", aid, "rid", rid, "uid", uid);
            obj.put("actionId", actionId);
        }
    }

    public String[] getReviewDue(long jid){
        String responseDue = "";
        String reviewDue = "";

        JournalSetting response = callStub.callStub("getSetting",JournalSetting.class,"journalId",jid,"configPoint","Response Due");
        if( response == null){
            responseDue = jConfig.getConfig("Response Due").getConfigContent();
        }else{
            responseDue = response.getConfigContent();
        }

        JournalSetting review = callStub.callStub("getSetting",JournalSetting.class,"journalId",jid,"configPoint","Review Due");
        if( review == null){
            reviewDue = jConfig.getConfig("Review Due").getConfigContent();
        }else{
            reviewDue = review.getConfigContent();
        }
        Date current = new Date(); 
        Date responseDate = new Date( current.getTime() + 24*3600*1000*Integer.valueOf(responseDue));
        Date reviewDate = new Date( current.getTime() + 24*3600*1000*Integer.valueOf(reviewDue));
        String strDateFormat = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        
        return new String[] {
          sdf.format(responseDate) ,
          sdf.format(reviewDate) ,
        };

    }

    public void analysis(ReviewRound rr, List<ArticleHistory> histories, boolean isZH){
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
        //处理过渡建议状态，过渡一下,判断依据是review-suggest， 出现这个状态，是编辑建议
        //然后取出建议，再放入RR的SUGGEST中，然后再取下一条
        if( current.getStatus().equals("review-suggest")){
            rr.setSuggest(current.getDesc());
            int index = 1;
            current = histories.get(index);
            while(current.getStatus().equals("review-suggest")){
                index ++;
                current = histories.get(index); //取下一条
            }
        }
        rr.setLastStats(current.getStatus());
        rr.setDesc(current.getDesc());
        rr.setHasPreview(false);

        rr.setCanModify(true);
        
        int statusIndex = ReviewStateMachine.getStatusIndex(current.getStatus());
        rr.setCurstate(statusIndex);
        if( statusIndex > 2 ) {//判断本轮是否结束
            rr.setEnd(true);
            rr.setDecision( ReviewRoundDecision.getByIndex(statusIndex).getTitle(isZH) );
            if( statusIndex != 6){
                Message m = callStub.callStub("getMessage", Message.class, "id", current.getMsgId());
                rr.setDecisionMsg(m);

                if(m.getAppendsJSONStr()!=null){
                    JSONArray files = JSONObject.parseArray( m.getAppendsJSONStr());
                    if( files.size() > 0 ){
                        rr.setDecisionFiles( files );
                    }
                }
                //是否是REVISION，自己根据RR.DECSIION值来定                
            }
        }
        else rr.setEnd(false);

        List<JSONObject> logs = new LinkedList<>(); //日志倒序
        //组织LOG，并判断是否有预审,这个后面改了，只以REVIEW，这些大阶段加轮次为依据取文件
        long fileAhId = -1;
        for(ArticleHistory ah:histories){
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            JSONObject obj = new JSONObject(); 
            obj.put("date",  df.format(ah.getStamp()));
            obj.put("status",  ah.getStatus());
            obj.put("desc", ah.getDesc());
            logs.add(obj);

            if( ah.getStatus().equals("review-pre-review") ){
                rr.setHasPreview(true);
                fileAhId = ah.getId();  //如果有PRE-REVIEW，文件以PREVIEW轮为准，如果没有以最低ID轮为准
            }
        }
        rr.setLogs(logs);

        //判断文件是否能修改
        if( rr.isHasPreview() &&  statusIndex >= 1 ){
            rr.setCanModify(false);
        }else{
            if( statusIndex > 1 ){
                rr.setCanModify(false);
            }
        }
        //设置FILES
        if( fileAhId == -1 ){
            fileAhId = histories.get(histories.size()-1).getId();
        }
        rr.setFiles(afHelper.getFiles(fileAhId));


        //取本轮的审稿信息
        String reviewJson = callStub.callStub("getReviewActionByAidAndRid", String.class, "aid", rr.getAid(), "rid", rr.getIndex());
        System.out.println(reviewJson);
        JSONArray reviewActionArr = JSONArray.parseArray(reviewJson);
        List<JSONObject> reviewActions = new LinkedList<>();
        for(int i=0; i<reviewActionArr.size(); i++){ 
            JSONObject t = reviewActionArr.getJSONObject(i);
            String sd = t.getString("responseDue"); String rd = t.getString("reviewDue");
            sd = sd.split(" ")[0]; sd = sd.substring(sd.indexOf("-")+1);
            rd = rd.split(" ")[0]; rd = rd.substring(rd.indexOf("-")+1);
            t.put("responseDue", sd);
            t.put("reviewDue", rd);

            int status = t.getIntValue("curstate");
            t.put("status", ReviewAction.status[status][isZH?1:0]);

            //判断是否被关闭使用的是CLOSED标志，如果是CLOSE，设置为关闭
            if( t.getBooleanValue("closed") ){
                t.put("status", ReviewAction.status[4][isZH?1:0]);
            }

            t.put("hasResult", false);
            t.put("overdue", false);
            t.put("end", false);
            t.put("result", "");

            //如果审稿结束此时可以查看结果
            if( status == ReviewAction.COMPLETE ){
                t.put("hasResult", true);
            }
            //如果逾期，可以催信
            if( status == ReviewAction.RESPONSEOVERDUE || status == ReviewAction.REVIEWOVERDUE ){
                t.put("overdue", true);
            }
            //如果审稿结束或者被关闭，END， 此时可以回撤
            if( status == ReviewAction.COMPLETE || t.getBooleanValue("closed") ){
                t.put("end", true);
            }else{
                t.put("end", false);//不在这二种状态的，都可以CLOSE
            }


            if(t.getIntValue("resultType") == 0){
                t.put("result", "-" );
            }else{
                ReviewRecommendType result = ReviewRecommendType.getByIndex( t.getIntValue("resultType"));
                t.put("result", isZH ? result.getZh() : result.getEn() );
            }
            reviewActions.add(t);
        }   
        
        rr.setReviewActions(reviewActions);
        //取本轮的讨论消息
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

    //拿到HISTORY，并转换成REVIEWROUN列表,并返回
    public List<ReviewRound> getRounds( long aid, boolean isZH ){
        //返回按ID倒序排的HISTORY列表
        List<ArticleHistory> histories = helper.getHistoryByAidandFlow(aid, "REVIEW");
        //通过ROUND字段来区分不同的ROUND
        List<ReviewRound> rounds = new LinkedList<>();
        
        int currentRound = -1;
        ReviewRound rr = null;
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
                rr = new ReviewRound();
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
