package edu.nuist.ojs.middle.workflow;

import java.util.HashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.article.Article;
import edu.nuist.ojs.common.entity.article.ArticleAuthor;
import edu.nuist.ojs.middle.resourcemapper.emailtpl.EmailTplVariableMapper;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalConfig;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class ArticleInfoHelper {

    @Autowired
    private CallStub callStub;

    @Autowired
    private JournalConfig jConfig; 

    @Autowired
    private EmailTplVariableMapper tplVariableMapper;

    public HashMap<String, String> getVariableMap(JSONObject obj){
        Long pid = null, jid=null, aid=null, jsid=null;
        if( obj.get("p") != null ){ pid = obj.getLongValue("p"); }
        if( obj.get("j") != null ){ jid = obj.getLongValue("j"); }
        if( obj.get("a") != null ){ aid = obj.getLongValue("p"); }
        if( obj.get("js") != null ){ jsid = obj.getLongValue("js"); }
        return getVariableMap( pid,  jid,  aid,  jsid);
    }

    public HashMap<String, String> getVariableMap(Long aid){
        String jsonStr = callStub.callStub("getArticleById", String.class, "aid", aid);
        long jid = JSON.parseObject(jsonStr).getLongValue("jid");
        long pid = JSON.parseObject(jsonStr).getLongValue("pid");
        return getVariableMap(pid, jid, aid, jid);
    }

    public HashMap<String, String> getVariableMap(Long pid, Long jid, Long aid, Long jsid){
        JSONObject obj = new JSONObject();
        if(pid != null) obj.put("p", pid);
        if(jid != null) obj.put("j", jid);
        if(aid != null) obj.put("a", aid);
        if(jsid != null) obj.put("js", jsid);

        JSONObject datas = callStub.callStub("getEmailVariables", JSONObject.class, "paraTagInJsonStr", JSONObject.toJSONString(obj));

        HashMap<String, String>  rst = new HashMap<>();
        if(pid != null) rst.putAll(getPublisherVariables( datas.getJSONObject("p") ));
        if(jid != null) rst.putAll(getJournalVariables( datas.getJSONObject("j") ));
        if(aid != null) rst.putAll(getArticleVariables( datas.getJSONObject("a"), datas.getJSONObject("submitor") ));
        if(jsid != null) rst.putAll(getJournalSettingVariables( datas.getJSONArray("js") ));
        return rst;
    }

    public HashMap<String, String> getPublisherVariables(JSONObject obj){
        HashMap<String, String> params = tplVariableMapper.getTplvariables().get("publisher");
        HashMap<String, String> rst = new HashMap<>();
        for(String key : params.keySet()){
            String value = params.get(key);
            rst.put(key, obj.getString(value));
        }
        
        if( rst.get("#pi18n#") != null ){
            rst.put("i18n", rst.get("#pi18n#"));
        }


        return rst;
    }

    public HashMap<String, String> getJournalVariables(JSONObject obj){
        HashMap<String, String> params = tplVariableMapper.getTplvariables().get("journal");
        HashMap<String, String> rst = new HashMap<>();
        for(String key : params.keySet()){
            String value = params.get(key);
            rst.put(key, obj.getString(value));
        }
        return rst;
    }

    public HashMap<String, String> getArticleVariables(JSONObject article, JSONObject submitor){
        HashMap<String, String> params = tplVariableMapper.getTplvariables().get("article");
        HashMap<String, String> rst = new HashMap<>();
        
        Article a = JSONObject.toJavaObject(article, Article.class);
        String authorsName = "";
        String authorsEmail = "";

        String correspondingName = "";
        String correspondingEmail = "";

        for(ArticleAuthor aa : a.getAuthors()){
            authorsName += aa.getName() + ";";
            authorsEmail += aa.getEmail() + ";";

            if(aa.isCorresponding()){
                correspondingName += aa.getName() + ";";
                correspondingEmail += aa.getEmail() + ";";
            }
        }
        authorsEmail = authorsEmail.substring(0, authorsEmail.length()-1);
        authorsName = authorsName.substring(0, authorsName.length()-1);
        correspondingName = correspondingName.substring(0, correspondingName.length()-1);
        correspondingEmail = correspondingEmail.substring(0, correspondingEmail.length()-1);

        for(String key : params.keySet()){
            String value = params.get(key);
            if( value.indexOf(".") == -1 )
                rst.put(key, article.getString(value));
            else{
                if(value.indexOf("submit") != -1){
                    if(value.indexOf("name") != -1 ) rst.put(key, submitor.getString("username"));
                    if(value.indexOf("email") != -1 ) rst.put(key, submitor.getString("email"));
                    if(value.indexOf("userId") != -1 ) rst.put(key, submitor.getString("userId"));
                }else if(value.indexOf("authors") != -1){
                    if(value.indexOf("names") != -1 ) rst.put(key, authorsName);
                    if(value.indexOf("emails") != -1 ) rst.put(key, authorsEmail);
                }else if(value.indexOf("corresponding") != -1){
                    if(value.indexOf("name") != -1 ) rst.put(key, correspondingName);
                    if(value.indexOf("email") != -1 ) rst.put(key, correspondingEmail);
                }
            }
        }
        return rst;

    }

    public HashMap<String, String> getJournalSettingVariables(JSONArray obj){
        HashMap<String, String> params = tplVariableMapper.getTplvariables().get("journasetting");
        HashMap<String, String> rst = new HashMap<>();
        for(String key : params.keySet()){
            String value = params.get(key);
            if( value.indexOf(".") != -1 ){
                String[] loc = value.split("\\.");
                for(int i=0; i<obj.size(); i++){
                    if( loc[0].equals(obj.getJSONObject(i).getString("configPoint"))){
                        JSONObject values = obj.getJSONObject(i).getJSONObject("configContent");
                        rst.put(key,  values.getString(loc[1]));
                    };
                } 
            }else{
                for(int i=0; i<obj.size(); i++){
                    if( value.equals(obj.getJSONObject(i).getString("configPoint"))){
                        rst.put(key,  obj.getJSONObject(i).getString("configContent"));
                    };
                } 
            }
        }    
        /**如果系统中没有设置，则直接从默认配置中拿出来
         * '[#Review Response Due#]': 'Revision Due'
        '[#Review Due#]': 'Review Due'
        [#Revision Due#]': 'Revision Due'
         *  */     
        if( rst.get("#Review Response Due#") == null ){
            rst.put("#Review Response Due#", jConfig.getConfig("Response Due").getConfigContent());
        }

        if( rst.get("#Review Due#") == null ){
            rst.put("#Review Due#", jConfig.getConfig("Review Due").getConfigContent());
        }

        if( rst.get("#Revision Due#") == null ){
            rst.put("#Revision Due#", jConfig.getConfig("Revision Due").getConfigContent());
        }

        //info中也有国际化的参数
        if( rst.get("#ji18n#") != null ){
            rst.put("i18n", rst.get("#ji18n#"));
        }

        return rst;
    }

}
