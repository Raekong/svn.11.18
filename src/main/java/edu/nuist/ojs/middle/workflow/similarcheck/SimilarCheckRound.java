package edu.nuist.ojs.middle.workflow.similarcheck;

import java.util.List;

import com.alibaba.fastjson.JSONObject;

import edu.nuist.ojs.common.entity.SimilarCheck;
import lombok.Data;

@Data
public class SimilarCheckRound {
    private long aid;
    private long roundId;
    private int index;
    private int curstate;
    private String lastStats;
    private String desc;
    private boolean hasPreview;

    private boolean uploaded;//表示本轮已经进入结束状态，但是也可能处于待提交修改稿结束状态
    private boolean ended;
    private boolean closed; //表示本轮已经彻底结束，有新一轮诞生

    private SimilarCheck sc ; //表示本轮查重的SC ID

    private List<JSONObject> previewFiles; 

}
