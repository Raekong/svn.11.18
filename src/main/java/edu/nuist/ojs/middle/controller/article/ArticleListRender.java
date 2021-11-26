package edu.nuist.ojs.middle.controller.article;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.middle.resourcemapper.article.ArticleStatusMapper;

@Component
public class ArticleListRender {

    @Autowired
    private ArticleStatusMapper statusMapper; 
    
    public JSONObject getRendData( List<String> filter, JSONObject obj, JSONObject showSetting){
        JSONObject all = new JSONObject();

        for(Entry<String, Object> e : showSetting.entrySet()){
            String type = e.getKey();
            if( !filter.contains(type) ) continue; //执行过滤，去掉用户权限之外的数据

            JSONObject setting = showSetting.getJSONObject(type);
            JSONObject value = obj.getJSONObject(type);
            if( value == null){ //过滤掉没有数据的部分，虽然用户有权，但是这部分没有形成数据
                //all.put(type, new JSONObject());
                continue;
            }

            JSONObject rst = new JSONObject();
            boolean flag = false; //用户有权看，但是没有配置的标志
            for(Entry<String, Object> point: setting.entrySet()){
                String key = point.getKey();
                if( ((String)point.getValue()).equals("true")){
                    flag = true;
                    JSONObject rendValue = renderByShowSettingPoint(type, value, point.getKey());
                    rst.put(key, rendValue);
                }
            }
            //只有用户配置要看，才放进去
            if(flag) all.put(type, rst);
        }   
        
        return all;
    }

    //获得其中一个渲染
    public JSONObject renderByShowSettingPoint(String type, JSONObject obj, String point){

        HashMap<String,String[]> settingTable = POINT_OBJMAPPER.get(type);
        if(settingTable == null || settingTable.get(point)==null) return new JSONObject();
        String[] keys = settingTable.get(point);
        JSONObject rst = new JSONObject();
        if( keys[0].equals("call")){ //不能直接取要调用方法来转换
            rst.put(keys[1], converData(keys[1], obj));
        }else{
            for(String key : keys){
                rst.put(key, obj.getString(key));
            }
        }

        return rst;
    }

    public String converData(String key, JSONObject obj){
        String rst = "";
        switch( key ){
            case "status":
                rst = statusMapper.getByIndex(obj.getIntValue("sindex")).getStatusEN();
                break;
            case "authors":
                JSONArray arr = obj.getJSONArray("authors");
                for(int i=0; i<arr.size(); i++){
                    rst += arr.getJSONObject(i).getString("name") + "[" + arr.getJSONObject(i).getString("email") + "]; ";
                }
                break;
            case "reviewtotalround":
                rst = (obj.getJSONArray("rounds") == null ? 0 : obj.getJSONArray("rounds").size()) + "" ;
                break;
            case "reviewlaststatis":
                JSONObject t  = new JSONObject();
                JSONObject laststatis = obj.getJSONObject("lastRound");
                if( laststatis != null ){
                    t.put("total", laststatis.getString("total"));
                    t.put("completed", laststatis.getString("completed"));
                    t.put("reviewing", laststatis.getString("reviewing"));
                    t.put("overdue", laststatis.getString("overdue"));
                    t.put("decline", laststatis.getString("decline"));
                }
                rst = t.toJSONString();
                break;
            case "reviewers":
                arr = new JSONArray();
                laststatis = obj.getJSONObject("lastRound");
                if( laststatis != null ){
                    arr = laststatis.getJSONArray("reviewers");
                    for(int i=0; i<arr.size(); i++){
                        rst += arr.getJSONObject(i).getString("name") + "[" + arr.getJSONObject(i).getString("email") + "]; ";
                    }
                }
                break;
            case "paststatis":
                arr = new JSONArray();
                JSONArray rounds = obj.getJSONArray("rounds");
                if( rounds != null){
                    for(int i=0; i<rounds.size(); i++){
                        JSONObject tmp = new JSONObject();
                        JSONObject round = rounds.getJSONObject(i);
                        tmp.put("total", round.getString("total"));
                        tmp.put("completed", round.getString("completed"));
                        tmp.put("reviewing", round.getString("reviewing"));
                        tmp.put("overdue", round.getString("overdue"));
                        tmp.put("decline", round.getString("decline"));
                        arr.add(tmp);
                    }
                }
                rst = arr.toJSONString();
                break;
            case "checks":
                rst = obj.getJSONArray("checks").toJSONString();
                break;
        }
        return rst;
    }

    

    public  final HashMap<String, String[]> ARTICLE_MAP = new HashMap<String, String[]>(){
        {   
            put("id", new String[]{ "aid" }  );
            put("title", new String[]{  "title"}  );
            put("status", new String[]{ "call", "status" }  );
            put("js", new String[]{ "call","js"}  );
            put("subdate", new String[]{ "subdate"}  );
            put("sumbitor", new String[]{ "subname","subemail"}  );
            put("editor", new String[]{ "ename","eemail"}  );
            put("authors", new String[]{ "call", "authors"}  );
        }
    };

    public  final HashMap<String, String[]> REVIEW_MAP = new HashMap<String, String[]>(){
        {
            put("statis", new String[]{ "call","reviewlaststatis"}  );
            put("totalrun", new String[]{ "call","reviewtotalround" }  );
            put("startdate", new String[]{ "startdate" }  );
            put("editor", new String[]{ "ename", "eemail" }  );
            put("reviewers", new String[]{  "call", "reviewers"}  );
            put("paststatis", new String[]{ "call", "paststatis"}  );
        }
    };

    public  final HashMap<String, String[]> PAYMENT_MAP = new HashMap<String, String[]>(){
        {
            put("totalpage", new String[]{ "totalpage"}  );
            put("totalapc", new String[]{ "apc" }  );
            put("totalpaid", new String[]{ "call", "totalpaid" }  );
            put("editor", new String[]{ "ename", "eemail" }  );
            put("startdate", new String[]{  "startdate"}  );
            put("payemail", new String[]{ "" }  );
            put("history", new String[]{ "call", "history"}  );
        }
    };

    public  final HashMap<String, String[]> SIMILAR_MAP = new HashMap<String, String[]>(){
        {
            put("lastresult", new String[]{  "call", "checks"}  );
            put("totalrun", new String[]{ "totalrounds" }  );
            put("startdate", new String[]{ "startdate" }  );
            put("editor", new String[]{ "ename", "eemail" }  );
        }
    };

    public  final HashMap<String, String[]> COPYEDIT_MAP = new HashMap<String, String[]>(){
        {
        }
    };

    public  final HashMap<String, HashMap<String, String[]>> POINT_OBJMAPPER = 
        new HashMap<String, HashMap<String, String[]>>(){
            {
                put("article", ARTICLE_MAP);
                put("review", REVIEW_MAP);
                put("payment", PAYMENT_MAP);
                put("similar", SIMILAR_MAP );
                put("copyedit", COPYEDIT_MAP);
            }
    };
}
