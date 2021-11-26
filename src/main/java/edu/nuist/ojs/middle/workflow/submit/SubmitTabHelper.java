package edu.nuist.ojs.middle.workflow.submit;

import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.EmailFile;
import edu.nuist.ojs.common.entity.article.Article;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;

@Component
public class SubmitTabHelper {
    @Autowired
    private CallStub callStub;

    @Autowired
    private MessageComponent mComponent;

    @Autowired
    private ArticleFileHelper aFileHelper;

    @Autowired
    private HistoryHelper helper; 
    
    public HashMap<String, Object> render(long aid){
        HashMap<String, Object> rst = new HashMap<String, Object>();

        String a = callStub.callStub("getArticleById", String.class, "aid", aid);
        Article article = Article.getArticle(JSONObject.parseObject(a));

        ArticleHistory ah = helper.getHistory( aid, "submit", 0);
        
        List<ArticleFile> files = aFileHelper.getFiles(ah.getId());
        for(ArticleFile file : files){
            file.setInnerId( mComponent.getFileUrl(file.getInnerId()));
        }

        article.setFiles(files);
        rst.put("a", article);

        rst.put("modifyFile", aFileHelper.canModify(aid, "submit", 0));
        return rst;
    }
}
