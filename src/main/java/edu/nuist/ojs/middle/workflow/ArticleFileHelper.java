package edu.nuist.ojs.middle.workflow;


import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.common.entity.article.ArticleHistory;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.resourcemapper.journal.JorunalFileType;
import edu.nuist.ojs.middle.stub.CallStub;


@Component
public class ArticleFileHelper {
    @Autowired
    private CallStub callStub;
    
    @Autowired
    private HistoryHelper helper;

    @Autowired
    private MessageComponent mComponent;   

    
    @Autowired
    private JorunalFileType jfileTypes; 

     /**
     * 创建一个新的ROUND，要先安排好文件和审稿人才行
     * @param aid
     * @param status
     * @param round
     */
    public void serizalFileFromLastStatus(ArticleHistory ah){
        //转移文件
        List<ArticleFile> files = getLastRoundFiles(ah.getAid(), ah.getStatus(), ah.getRound());
        for(ArticleFile file : files){
            file.setVersion( ah.getFileVersion() );
        }
        serizalFiles(files);
    }

    /**
     * 产生新一轮审稿时文件附带过来了 
     */
    public void serizalNewCopyeditRoundFiles(long aid, int rid, List<ArticleFile> afs){
        List<ArticleFile> files = getFileForReviewRound( aid,   rid);//上一轮
        List<ArticleFile> rst = new LinkedList<>();
        for(ArticleFile af : afs){
            for(ArticleFile file : files){
                if( af.getFileType().equals(file.getFileType()) //文件类型相等，且不属于补充文件类型的，应该从上一轮文件中去除
                    && af.getFileType().indexOf("Files") == -1
                ){
                    rst.add( file );
                }
            }
        }
        //去掉重复的文件,并形成新的文件集
        files.removeAll(rst);
        afs.addAll(files);
        for(ArticleFile af : afs){
            af.setVersion("COPYEDIT-" + ( rid+ 1) );
        }
        serizalFiles(afs);
    }

    public void serizalNewReviewRoundFiles(long aid, int rid, List<ArticleFile> afs){
        List<ArticleFile> files = getFileForReviewRound( aid,   rid);//上一轮
        List<ArticleFile> rst = new LinkedList<>();
        for(ArticleFile af : afs){
            for(ArticleFile file : files){
                if( af.getFileType().equals(file.getFileType()) //文件类型相等，且不属于补充文件类型的，应该从上一轮文件中去除
                    && af.getFileType().indexOf("Files") == -1
                ){
                    rst.add( file );
                }
            }
        }
        //去掉重复的文件,并形成新的文件集
        files.removeAll(rst);
        afs.addAll(files);
        for(ArticleFile af : afs){
            af.setVersion("REVIEW-" + ( rid+ 1) );
        }
        
        serizalFiles(afs);
    }

    public void serizalFiles(List<ArticleFile> files){
        for(ArticleFile file : files){
            if(file.getFileType() != null){ //付款文件不在论文稿件中，因此要过滤掉
                callStub.callStub("articlefileupload", String.class,
                "filetype", file.getFileType(), 
                "originName", file.getOriginName(), 
                "innerId", file.getInnerId(),
                "aid", file.getAid(),
                "version", file.getVersion()
            );
            }
            
        }
    }

    public ArticleFile saveFile(ArticleFile file){
        return callStub.callStub("saveArticleFile", ArticleFile.class,"file", file
        );
    }

    public ArticleFile getArticleFileById(long fid){
        return callStub.callStub("getArticleFileById", ArticleFile.class, "fid", fid);
    }

    //判断本轮是否具有修改文章的能力
    public boolean canModify(long aid, String status, int round){
        ArticleHistory ah = helper.getNextHistory(aid, status, round); 
        if( ah==null ) return true;
        List<ArticleFile> files = getFiles(ah.getId());
        return files==null || files.size()==0;
    }

    public void delFile(long fid){
        callStub.callStub("delArticleFile", String.class, "fid" , fid);
    }

    public List<ArticleFile> getLastRoundFiles(long aid, String status, int round){
        ArticleHistory ah = helper.getLastHistory(aid, status, round); 
        return getFiles(ah.getId());
    }

    public List<ArticleFile> getFileForReviewRound(long aid,  int round){
        String jsonStr = callStub.callStub("getFileForReviewRound", String.class, "aid", aid, "rid", round);
        if( jsonStr == null ) return null;
        JSONArray arr = JSONArray.parseArray(jsonStr);
        List<ArticleFile> rst = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            ArticleFile file = JSONObject.toJavaObject(arr.getJSONObject(i), ArticleFile.class);
            rst.add(file);
        }
        return rst;
    }

    public List<ArticleFile> getFileForCopyeditRound(long aid,  int round){
        String jsonStr = callStub.callStub("getFileForCopyeditRound", String.class, "aid", aid, "rid", round);
        if( jsonStr == null ) return null;
        JSONArray arr = JSONArray.parseArray(jsonStr);
        List<ArticleFile> rst = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            ArticleFile file = JSONObject.toJavaObject(arr.getJSONObject(i), ArticleFile.class);
            rst.add(file);
        }
        return rst;
    }
    
    
    //拿到文章某一轮所有的文件信息
    public List<ArticleFile> getFiles(long ahid){ 
        String jsonStr = callStub.callStub("getArticleFilesByAHId", String.class, "ahid", ahid);

        if( jsonStr == null ) return null;
        JSONArray arr = JSONArray.parseArray(jsonStr);
        List<ArticleFile> rst = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            ArticleFile file = JSONObject.toJavaObject(arr.getJSONObject(i), ArticleFile.class);
            file.setVersion( mComponent.getFileUrl(file.getInnerId()));//借用作URL
            
            rst.add(file);
        }
        return rst;
    }

    public List<ArticleFile> getPaymentFiles(long aid){
        String version = "PAYMENT-0";
        String jsonStr = callStub.callStub("getArticleFilesByAidAndVersion", String.class, "aid", aid, "version", version);

        JSONArray arr = JSONArray.parseArray(jsonStr);
        List<ArticleFile> files = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            ArticleFile file = JSONObject.toJavaObject(arr.getJSONObject(i), ArticleFile.class);
            file.setVersion( mComponent.getFileUrl(file.getInnerId()));//借用作URL
            files.add(file);           
        }

        return files;
    }

    public ArticleFile getSimilarCheckFile(long aid, int round){
        String version = "SIMILARCHECK-" + round;
        String jsonStr = callStub.callStub("getArticleFilesByAidAndVersion", String.class, "aid", aid, "version", version);

        JSONArray arr = JSONArray.parseArray(jsonStr);
        List<ArticleFile> files = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            ArticleFile file = JSONObject.toJavaObject(arr.getJSONObject(i), ArticleFile.class);
            files.add(file);           
        }

        ArticleFile af = jfileTypes.getManscriptInWord(files);
        if( af == null ){
            af = jfileTypes.getManscriptInPdf(files);
        }

        return af;
    }

    public ArticleFile findByAidAndVersionAndFileType(long aid, String version, String fileType){
       return callStub.callStub("findByAidAndVersionAndFileType", ArticleFile.class, "aid", aid, "version", version, "fileType", fileType);
    }
}
