package edu.nuist.ojs.middle.controller.article;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.datamonitor.datamonitormap.MonitorMapper;
import edu.nuist.ojs.middle.datamonitor.datamonitormap.MonitorQuery;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;

@Controller
public class ArticleListBoardController {
    
    @Autowired
    private MonitorMapper monitorMapper;

    @Autowired
    private CallStub callStub;

    @Autowired
    private ArticleListRender render;

    @Autowired
    private ArticleListQueryComponent component; 

    @RequestMapping("/article/list/query")
    @ContextAnnotation(configPoint = "articlelist", configKeys = "u.userId,u.i18n,u.root,u.publisherId,p.i18n")
    @ResponseBody
    public JSONObject listQuery(  
        HttpSession session,
        @RequestParam  int  pageNumber,
        @RequestParam  int pageSize,
        @RequestParam String conds 
    ){  
        JSONArray arr = JSONArray.parseArray(conds);
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        long pid = ThymleafHelper.get(session, "publisherId", long.class); 

        Set<Long> rst = new HashSet<>();
        for(int i=0; i<arr.size(); i++){
            Set<Long> tmp = component.execQuery(arr.getJSONObject(i), uid);
            String logic = arr.getJSONObject(i).getString("logic");
            if( logic == null){ //第一条语句
                rst.addAll(tmp);
            }else{
                if( logic.equals("And")){
                    rst.retainAll(tmp);
                }else{
                    rst.addAll(tmp);
                }
            }
        }
        return packageToPage( uid, rst, getTableFilter ( uid,  pid), pageNumber, pageSize);
    }

    public JSONObject getUserShowSetting(long uid){
        JSONObject obj = callStub.callStub("getlistboardsetting", JSONObject.class, "uid", uid);
        if( obj == null){
            obj = new JSONObject();
            return monitorMapper.getSysConfigList();
        }
        return obj;
    }

    //根据用户的权限来控制显示的字段
    private List<String> getTableFilter (long uid, long pid){
        JSONObject showList = userRoleHelper.getEditorListBoard(pid, uid);
        List<String> showFilter = new LinkedList<String>();
        showFilter.add("article");
        
        if(showList.getBooleanValue("manager") || showList.getBooleanValue("editor")){
            showFilter.add("review");
        }

        if(showList.getBooleanValue("manager") || showList.getBooleanValue("similarcheck")){
            showFilter.add("similar");
        }

        if(showList.getBooleanValue("manager") || showList.getBooleanValue("financial")){
            showFilter.add("payment");
        }

        if(showList.getBooleanValue("manager") || showList.getBooleanValue("copyeditor")){
            showFilter.add("copyedit");
        }

        return showFilter;
    }

    private JSONObject packageToPage(long uid, Set<Long> aids, List<String> filter, int  pageNumber, int pageSize){
        List<Long> list_1 = new ArrayList<>(aids);
        list_1.sort(Comparator.reverseOrder());//降序

        int total = list_1.size();
        int totalPage = list_1.size() / pageSize;
        if( list_1.size() % pageSize != 0 ) totalPage += 1;

        JSONArray arr = new JSONArray();
        for(int i=(pageNumber-1)*pageSize; i<total; i++){
            arr.add( list_1.get(i));
        }
        JSONArray infos = callStub.callStub("getMonitorData", JSONArray.class, "ids", arr.toJSONString(), "uid", uid);

        JSONObject showsettings = getUserShowSetting(uid);
        JSONArray datas = new JSONArray();
        for(int i=0; i<infos.size(); i++){
            //使用FILTER来过滤掉不能显示的数据
           datas.add( render.getRendData(filter, infos.getJSONObject(i), showsettings));
        }
        
        JSONObject obj = new JSONObject();
        obj.put("total", total);
        obj.put("totalPage", totalPage);
        obj.put("content", datas);
        return obj;
    }


    @RequestMapping("/article/list/querySetting/{table}")
    @ResponseBody
    public HashMap<String, MonitorQuery> querySetting(  
        HttpSession session,
        @PathVariable String table
    ){
        return monitorMapper.getCatagorys().get(table);
    }



    /**
     * 这个表格的实现极为复杂
     * 首先去后台，拿这个用户可以访问的所有文章
     * 由于用户权限存在管理员与普通编辑，所以在表上，管理员的所有列都能看，而普通编辑只能看自己的页
     * 1. 先根据用户的权限搞定CONFIGBOARD，再搞定TABLE中可以显示的列，这个在ARTICLEUSERROLEHELPER中搞定，然后用THEMLEAF来渲染
     * 2. 然后，再取出文章，再按文章与用户的权限，对可以显示的列内容进去过滤
     * 3. 再考虑用户自己的显示配置，设置到显示的列，再进行取舍
     */

    @Autowired
    private ArticleUserRoleHelper userRoleHelper; //执行用户能检索的表格


    @RequestMapping("/article/editor/list")
    @ResponseBody
    @ContextAnnotation(configPoint = "articlelist", configKeys = "u.userId,u.i18n,u.root,u.publisherId,p.i18n")
    public JSONObject getListForEditor(        
        @RequestParam  int  pageNumber,
        @RequestParam  int pageSize,
        HttpSession session
    ){
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        String json = callStub.callStub("getArticleByType", String.class, "type", "editor", "uid", uid, "page", pageNumber, "size", pageSize);
        JSONArray obj = JSONObject.parseObject(json).getJSONArray("content"); 
        //String i18n = ThymleafHelper.get(session, "i18n", String.class);
        HashSet<Long> arr = new HashSet<Long>();
        for(int i=0; i<obj.size(); i++){
            JSONObject o = obj.getJSONObject(i);
            arr.add( o.getLong("aid"));
        }
        
        
        //在此根据用户权限执行数据的过滤。整个前端的渲染也是根据这个数据来的
        long pid = ThymleafHelper.get(session, "publisherId", long.class); 
        return packageToPage( uid, arr, getTableFilter(uid, pid), pageNumber, pageSize);
        

    }

    @RequestMapping("/article/listboard/showDetailInfos/{aid}")
    public String showDetailInfos(@PathVariable long aid){ 
        return "article/list-detail-pop";
    }


    @RequestMapping("/article/listboard/showconfig")
    @ResponseBody
    public HashMap<String, List<String[]>> getConfigList(){ 
        return monitorMapper.getShowConfigList();
    }

    @RequestMapping("/article/listboard/loadShowConfig")
    @ResponseBody
    public JSONObject loadShowConfig(HttpSession session){  
        long uid = ThymleafHelper.get(session, "userId", long.class);
        return callStub.callStub("getlistboardsetting", JSONObject.class, "uid", uid);
    }

    @RequestMapping("/article/listboard/saveShowConfig")
    @ResponseBody
    public JSONObject saveShowConfig(HttpSession session, String settingJson){  
        long uid = ThymleafHelper.get(session, "userId", long.class);
        return callStub.callStub("savelistboardsetting", JSONObject.class, "uid", uid, "jsonString", settingJson);
    }


}
