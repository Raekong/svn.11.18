package edu.nuist.ojs.middle.workflow.submit;

import java.io.IOException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.article.ArticleAuthor;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalConfig;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class SubmitConfigCheckor {

    @Autowired
    private JournalConfig journalconfig;

    @Autowired
    private CallStub callStub;

    @Autowired
    private MessageComponent mComponent;
    
    @Autowired
    private PaperAnlysis anlysis;

    private int reviewerCheck(long jid, JSONArray reviewers){
        boolean flag = Boolean.valueOf(
                            getJournalSetting(jid, JournalConfigPointEnum.Submit_Requirements).getConfigContent()
                        );
        if( flag ){
            String num = getJournalSetting(jid, JournalConfigPointEnum.Require_Reviewers_Num).getConfigContent();
            if( reviewers.size() < Integer.valueOf(num)){
                return Integer.valueOf(num);
            }
            return 0;
        }else{
            return 0;
        }

    }

    private int coverLetterCheck(long jid, JSONArray files) throws IOException, TikaException{
        boolean flag = Boolean.valueOf(
                            getJournalSetting(jid, JournalConfigPointEnum.CoverLetter_Check).getConfigContent()
                        );
        if( flag ){
            for(int i=0; i<files.size(); i++){
                ArticleFile af = JSONObject.toJavaObject( files.getJSONObject(i), ArticleFile.class);
                if( af.getFileType().toLowerCase().indexOf("cover") != -1 ){
                    String url = mComponent.getFileUrl( af.getInnerId() );
                    return anlysis.coverCheck(url) ? 0 : 1;
                }
            }
        }
        return 0;
    }

    private int checkCorresponingAuthor(JSONArray authors){
        boolean flag = false;
        for( int i=0; i<authors.size(); i++){
            ArticleAuthor author =  JSONObject.toJavaObject( authors.getJSONObject(i), ArticleAuthor.class);
           
            if( author.isCorresponding() ){
                flag = true;
                break;
            }
        }
        if( !flag ) return 1;
        else return 0;
    }

    private int materialCheck(long jid, JSONArray files){
        boolean coverletter = false;
        boolean pdf = false;
        boolean latex = false;
        boolean word = false;
        for( int i=0; i<files.size(); i++){
            ArticleFile af = JSONObject.toJavaObject( files.getJSONObject(i), ArticleFile.class);
            if( af.getFileType().toLowerCase().indexOf("pdf") != -1 ) pdf = true;
            if( af.getFileType().toLowerCase().indexOf("latex") != -1 ) latex = true;
            if( af.getFileType().toLowerCase().indexOf("word") != -1 ) word = true;
            if( af.getFileType().toLowerCase().indexOf("cover") != -1 ) coverletter = true;
        }
        
       
        boolean flag = Boolean.valueOf(
                            getJournalSetting(jid, JournalConfigPointEnum.Latex_Check).getConfigContent()
        );
        if(flag){//??????LATEX
            if( pdf && !latex ) return 1;  //???PDF???????????????LATEX??????
        }

        flag = Boolean.valueOf(
            getJournalSetting(jid, JournalConfigPointEnum.CoverLetter_Requirement).getConfigContent()
        );
        if(flag && !coverletter) return 0;     //???????????????????????????COVERLETTER,????????????COVERLETTER
        if( !word && !pdf ) return 2;  //?????????WORD?????????PDF??????
        return -1;
    }

    public String[] check(long jid, JSONObject json){
        int rst = reviewerCheck(jid, json.getJSONArray("reviewers"));
        if(rst > 0 ){ //???????????????????????????????????????????????????????????????????????????
            String[] t = SubmitErrorInfos.infos[3];
            t[0] += " " + rst; t[1] += " " + rst;
            return t;
        } 

        rst = materialCheck(jid, json.getJSONArray("uploadFiles"));
        if(rst >= 0 ) return SubmitErrorInfos.infos[rst];    //???????????????????????????????????????
        
        try {
            rst = coverLetterCheck(jid, json.getJSONArray("uploadFiles"));
        } catch (IOException | TikaException e) {
            e.printStackTrace();
            return SubmitErrorInfos.infos[4];  //???????????????????????????????????????Cover Letter???????????????APC
        }
        if(rst > 0 ) return SubmitErrorInfos.infos[4];  //Cover Letter???????????????APC

        rst = checkCorresponingAuthor(json.getJSONArray("authors"));
        if(rst > 0 ) return SubmitErrorInfos.infos[5]; //??????????????????
        return null;
    }

    private  JournalSetting  getJournalSetting(long jid, String configPoint){
        JournalSetting js = callStub.callStub("getSetting",JournalSetting.class,"journalId",jid,"configPoint",configPoint);

        if( js == null){
            js = journalconfig.getConfig(configPoint);
        }
        return js;
    }
}
