package edu.nuist.ojs.middle.controller.article;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class ArticleListQueryComponent {
    @Autowired
    private CallStub callStub;

    public static final String INT_SQL_TEMPLATE = "select i.aid from {0}  i, article_user au where i.{1}{2}{3} and au.aid=i.aid and au.uid={4}";
    public static final String Date_SQL_TEMPLATE = "select i.aid from {0}  i, article_user au where i.{1}{2}''{3}'' and au.aid=i.aid and au.uid={4}";
    public static final String String_SQL_TEMPLATE = "select i.aid from {0}  i, article_user au  where i.{1}  like ''%{2}%'' and au.aid=i.aid and au.uid={3}";
    public static final String Status_SQL_TEMPLATE = "select i.aid from article_infos i, article_user au where i.status in ({0}) and au.aid=i.aid and au.uid={1}";
    public static final String SimilarResult_SQL_TEMPLATE  = "select s.aid from similarcheck s where s.id in (select max(i.id) from similarcheck i, article_user au where i.pass={0} and au.aid=i.aid and au.uid={1} group by i.aid)";
    public static final String ReviewResult_SQL_TEMPLATE  = "select s.aid from review_round_info s where s.result like ''%{0}%'' and s.id in (select max(i.id) from review_round_info i, article_user au where au.aid=i.aid and au.uid={1} group by i.aid) ";


    public static final String Round_append = " order by i.id desc limit 0, 1";
    public static final String Pay_Round_append = " order by i.id";

    public Set<Long> execQuery(JSONObject obj, long uid){
        return query(getSQL(obj, uid));
    }   
 
    public Set<Long> query(String sql ){
        JSONArray arr = callStub.callStub("nativeQuery", JSONArray.class, "sql", sql);
        HashSet<Long> rst = new HashSet<>();
        for(int i=0; i<arr.size(); i++){
            rst.add(arr.getLong(i));
        }
        return rst;
    }
    
    public String getSQL(JSONObject obj, long uid){
        String table = obj.getString("table");
        String detail = obj.getString("detail");
        String oper = obj.getString("queryOper");
        String value = obj.getString("value");
        String type = obj.getString("type");
        String sql = "";

        //System.out.println( obj );
        if(detail.equals("status")){ //专门为STATUS准备的
            sql = getStatusSql(value, uid);
        }else{
            switch(type){
                case "int":
                    sql = getIntSql(table, detail, oper, value, uid);
                    break;
                case "checkresult":
                    value = value.toLowerCase().equals("pass") ? "1" : "0";
                    sql = MessageFormat.format(SimilarResult_SQL_TEMPLATE, value , uid);
                    break;
                case "reviewresult":
                    sql = MessageFormat.format(ReviewResult_SQL_TEMPLATE, value , uid);
                    break;
                case "date":
                    sql = getDateSql(table, detail, oper, value, uid);
                    break;
                case "string":
                    sql = getStringSql(table, detail, value, uid);
                    break;
            }
        }
        return sql;
    }


    public String getStatusSql(String value, long uid){
        String status = STATUS_INDEX_MAP.get(value);
        return MessageFormat.format(Status_SQL_TEMPLATE,  status, Long.valueOf(uid).toString());
    }

    public String getStringSql(String table, String detail, String value, long uid){
        String[] tmp = getTableAndField( table,  detail); //table and field
        if( tmp == null) return null; 
        String sql = MessageFormat.format(String_SQL_TEMPLATE, tmp[0], tmp[1], value.trim(), Long.valueOf(uid).toString());
        if(tmp[2] != null) sql += tmp[2];
        return sql;

    }

    public String getDateSql(String table, String detail, String oper, String value, long uid){
        String[] tmp = getTableAndField( table,  detail); //table and field
        if( tmp == null) return null; 
        String relationOper = "";
        switch( oper ){
            case "on":
                relationOper = "=";
                break;
            case "start":
                relationOper = ">=";
                break;
            case "end":
                relationOper = "<=";
                break;
        }

        String sql = MessageFormat.format(Date_SQL_TEMPLATE, tmp[0], tmp[1], relationOper, value ,Long.valueOf(uid).toString());
        if(tmp[2] != null) sql += tmp[2];
        return sql;
    }

    public String getIntSql(String table, String detail, String oper, String value, long uid){
        String[] tmp = getTableAndField( table,  detail); //table and field
        if( tmp == null) return null; 
        String relationOper = "";
        switch( oper ){
            case "=":
                relationOper = "=";
                break;
            case "≥":
                relationOper = ">=";
                break;
            case "≤":
                relationOper = "<=";
                break;
        }
        String sql = MessageFormat.format(INT_SQL_TEMPLATE, tmp[0], tmp[1], relationOper, value, Long.valueOf(uid).toString());
        if(tmp[2] != null) sql += tmp[2];
        return sql;
    }

    public String[] getTableAndField(String table, String detail){
        String[] rst = null;
        switch( table ){
            case "Article":
                rst = new String[3];
                rst[0] = "article_infos";
                rst[1] = TABLE_MAP.get("article_infos").get(detail);
                rst[2] = null;
                break;
            case "Review":
                HashMap<String, String> roundmap = TABLE_MAP.get("review_round_info");
                HashMap<String, String> reviewmap = TABLE_MAP.get("review_info");
                rst = new String[3];
                if( roundmap.get(detail) != null ){
                    rst[0] = "review_round_info";
                    rst[1] = roundmap.get(detail);
                    rst[2] = Round_append;
                 }else if(reviewmap.get(detail) != null){
                    rst[0] = "review_info";
                    rst[1] = reviewmap.get(detail);
                    rst[2] = null;
                }
                break;
            case "Payment":
                HashMap<String, String> payroundmap = TABLE_MAP.get("pay_info");
                HashMap<String, String> paymap = TABLE_MAP.get("payment_info");
                rst = new String[3];
                if( payroundmap.get(detail) != null ){
                    //这个不一样的，要仔细弄
                    rst[0] = "pay_info";
                    rst[1] = payroundmap.get(detail);
                    rst[2] = Pay_Round_append;
                }else if(paymap.get(detail) != null){
                    rst[0] = "payment_info";
                    rst[1] = paymap.get(detail);
                    rst[2] = null;
                }
                break;
            case "Similarity Check":
                HashMap<String, String> similaroundmap = TABLE_MAP.get("similarcheck");
                HashMap<String, String> similarmap = TABLE_MAP.get("similar_check_info");
                rst = new String[3];
                if( similaroundmap.get(detail) != null ){
                    rst[0] = "similarcheck";
                    rst[1] = similaroundmap.get(detail);
                    rst[2] = Round_append;
                }else if(similarmap.get(detail) != null){
                    rst[0] = "similar_check_info";
                    rst[1] = similarmap.get(detail);
                    rst[2] = null;
                }
                break;   
           
        }
        return rst;
    }


    public static final HashMap<String, String> STATUS_INDEX_MAP = new HashMap<String, String>(){ {
            put("Pre-revieww", "1,9");
            put("Reviewer required", "2");
            put("In Reviewing", "3");
            put("Revision Required", "2");
            put("Change Journal", "23");
            put("Payment Required", "17");
            put("Similarity Check", "11");
            put("Copyediting", "20,21");
            put("Accepted", "6");
            put("Declined", "5"); 
        }
    };

    public static final HashMap<String, String> ARTICLE_MAP = new HashMap<String, String>(){ {
            put("article id", "aid");
            put("title", "title");
            put("journal", "jtitle");
            put("section", "stitle");
            put("submit Date", "subdate");
            put("submitor name", "subname");
            put("submitor id", "sid");
            put("submitor email", "subemail");
            put("status", "status");
            put("status date", "lastupdate");
            put("pre-review editor name", "ename");
            put("pre-review editor id", "eid");
            put("pre-review editor email", "eemail");
            put("author email", "authors");
        }
    };

    public static final HashMap<String, String> REVIEWROUND_MAP = new HashMap<String, String>(){ {
            put("last round total request", "total");
            put("last round completed", "completed");
            put("last round reviewing", "reviewing");
            put("last round overdue", "overdue");
            put("last round decline", "decline");
            put("last round result", "result");
            put("reviewer name", "reviewers");
            put("reviewer email", "reviewers");
        }
    };

    public static final HashMap<String, String> REVIEW_MAP = new HashMap<String, String>(){ {
            put("total rounds", "totalrounds");
            put("start date", "startdate");
            put("editor name", "ename");
            put("editor id", "eid");
            put("editor email","eemail");
        }
    };

    public static final HashMap<String, String> PAYMENT_MAP = new HashMap<String, String>(){ {
            put("total paid","totalpaid");
            put("editor name","ename");
            put("editor id","eid");
            put("editor email","email");
            put("start date","startdate");
        }
    };

    public static final HashMap<String, String> PAYMENT_ROUND_MAP = new HashMap<String, String>(){ {
            put("payer email","pay_email");
            put("total page","org_page_number");
            put("total apc","org_totalapc");
            
        }
    };

    public static final HashMap<String, String> SIMILAR_ROUND_MAP = new HashMap<String, String>(){ {
            put("result", "pass");
            put("total", "total_similar");
            put("first check point", "frs_similar");
            put("second check point", "sec_similar");
            put("third check point", "thr_similar");
        }
    };

    public static final HashMap<String, String> SIMILAR_MAP = new HashMap<String, String>(){ {
            put("start date", "startdate");
            put("editor name","ename");
            put("editor id","eid");
            put("editor email","email");
        }
    };

    public static final HashMap<String, HashMap<String, String>> TABLE_MAP = new HashMap<String, HashMap<String, String>>(){
        {
            put("article_infos", ARTICLE_MAP);
            put("review_round_info", REVIEWROUND_MAP);
            put("review_info", REVIEW_MAP);
            put("payment_info", PAYMENT_MAP );
            put("pay_info", PAYMENT_ROUND_MAP);
            put("similarcheck", SIMILAR_ROUND_MAP);
            put("similar_check_info", SIMILAR_MAP );
        }
    };

    
}
