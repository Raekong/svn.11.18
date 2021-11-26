package edu.nuist.ojs.middle.workflow.review;

import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.common.entity.review.ReviewAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRound {
    private long aid;
    private long roundId;
    private int index;
    private int curstate;
    private List<JSONObject> reviewActions;
    private List<ArticleFile> files; 
    private String lastStats;
    private String desc;
    private boolean canModify;
    private boolean isEnd;//表示本轮已经进入结束状态，但是也可能处于待提交修改稿结束状态
    private boolean closed; //表示本轮已经彻底结束，有新一轮诞生
    private boolean hasPreview;

    private String suggest;

    private String decision;
    private Message decisionMsg;
    private JSONArray decisionFiles;

    private List<JSONObject> logs;
    private List<Message> msgs;

} 
