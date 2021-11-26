package edu.nuist.ojs.middle.workflow.copyedit;

import java.util.List;

import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import lombok.Data;

@Data
public class CopyeditRound {
    private long aid;
    private long roundId;
    private int index;
    private int curstate;

    private List<ArticleFile> files;  
    private String lastStats;
    private String desc;
    private boolean uploaded;//表示本轮已经进入结束状态，但是也可能处于待提交修改稿结束状态
    private boolean ended;
    private boolean closed; //表示本轮已经彻底结束，有新一轮诞生
    private List<Message> msgs;
}
