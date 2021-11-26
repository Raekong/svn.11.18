package edu.nuist.ojs.middle.journalsetting;

import edu.nuist.ojs.common.entity.Role;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalRole;
import edu.nuist.ojs.common.entity.journalsetting.JournalRoleEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.common.entity.journalsetting.JournalSettingParam;
import edu.nuist.ojs.middle.journalsetting.params.JournalEditor;
import edu.nuist.ojs.middle.journalsetting.params.JournalTeamSetting;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalConfig;
import edu.nuist.ojs.middle.stub.CallStub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;




/**
 * 辅助类，提供获取Journal Setting某个配置项的内容
 *      HashMap<String,  JournalSettingParam> setting， 这个参数代表一个期刊所有的配置
 *      key, 代表要取配置项的名称
 *      有待实现
 */
@Component
public class JournalSettingHelper {
    
    @Autowired
    private JournalConfig jConfig; 
    
    public int[] getSimilarCheckSetting( long jid ){
        List<JournalSetting> jss =  getAllSettingForJounral(jid); 
        HashMap<String, JournalSetting> rst = new HashMap<>();
        for(JournalSetting js : jss ){
            String cp = js.getConfigPoint();
            
            if( 
                cp.equals(JournalConfigPointEnum.Simaliary_Check)
                || cp.equals(JournalConfigPointEnum.Total_Similar)
                || cp.equals(JournalConfigPointEnum.First_Similar)
                || cp.equals(JournalConfigPointEnum.Second_Similar)
                || cp.equals(JournalConfigPointEnum.Third_Similar)
            ){
                
                rst.put(cp, js);
            }
        }
        
        if( rst.get(JournalConfigPointEnum.Simaliary_Check) == null || 
            rst.get(JournalConfigPointEnum.Simaliary_Check).getConfigContent().equals("false")
        ){
            return null;
        }

        int[] levels = new int[4];
        System.out.println( rst.get(JournalConfigPointEnum.Total_Similar));

        levels[0] = Integer.valueOf(jConfig.getConfig(JournalConfigPointEnum.Total_Similar).getConfigContent());
        if( rst.get(JournalConfigPointEnum.Total_Similar) != null){
            String content = rst.get(JournalConfigPointEnum.Total_Similar).getConfigContent();
            if(!"".equals(content)){
                levels[0] = Integer.valueOf(rst.get(JournalConfigPointEnum.Total_Similar).getConfigContent());
            }
        }

        levels[1] = Integer.valueOf(jConfig.getConfig(JournalConfigPointEnum.First_Similar).getConfigContent());
        if( rst.get(JournalConfigPointEnum.First_Similar) != null){
            String content = rst.get(JournalConfigPointEnum.First_Similar).getConfigContent();
            if(!"".equals(content)){
                levels[1] = Integer.valueOf(rst.get(JournalConfigPointEnum.First_Similar).getConfigContent());
            }
        }

        levels[2] = Integer.valueOf(jConfig.getConfig(JournalConfigPointEnum.Second_Similar).getConfigContent());
        if( rst.get(JournalConfigPointEnum.Second_Similar) != null){
            String content = rst.get(JournalConfigPointEnum.Second_Similar).getConfigContent();
            if(!"".equals(content)){
                levels[2] = Integer.valueOf(rst.get(JournalConfigPointEnum.Second_Similar).getConfigContent());
            }
        }

        levels[3] = Integer.valueOf(jConfig.getConfig(JournalConfigPointEnum.Third_Similar).getConfigContent());
        if( rst.get(JournalConfigPointEnum.Third_Similar) != null){
            String content = rst.get(JournalConfigPointEnum.Third_Similar).getConfigContent();
            if(!"".equals(content)){
                levels[3] = Integer.valueOf(rst.get(JournalConfigPointEnum.Third_Similar).getConfigContent());
            }
        }
        
        return levels;
    }


    public static <T> T get(HashMap<String,  JournalSettingParam> setting, String key, Class<T> tClass){
        return null;
    }

    public static Object get(HashMap<String,  JournalSettingParam> setting, String key){
        return null;
    }

    @Autowired
    private CallStub callStub;

    public List<JournalSetting> getAllSettingForJounral(long jid){
        String jsonStr = callStub.callStub("getallsetting", String.class, "journalId", jid);
        JSONArray arr = JSON.parseArray(jsonStr);
        List<JournalSetting> rst = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            rst.add(JSONObject.toJavaObject(arr.getJSONObject(i), JournalSetting.class));
        }
        return rst;
    }

    public User getJournalManager(long jid){
        return callStub.callStub("getmanager", User.class, "jid", jid);
    }

    public User getSectionEditor(long aid){
        return callStub.callStub("getsectionbyaid", User.class, "aid", aid); 
    }

    public User getEditor(long id){
        return callStub.callStub("findUserById", User.class, "id", id); 
    }

    public HashMap<Long, JournalRole[]> getRoleForUser(String jSettingsStr, String userEmail ){
       
        JSONObject roles = JSONObject.parseObject(jSettingsStr);
        HashMap<Long, JournalRole[]> rst = new HashMap<Long, JournalRole[]>();
        
        for(String key : roles.keySet()){
            Long jid = Long.valueOf(key);
            JSONArray arr = roles.getJSONArray(key);

            List<JournalRole> roleList = new LinkedList<>();
            for(int i=0; i<arr.size(); i++){
                //在系统的默认的角色中，角色的ID等同与其角色的顺序，这个ID与JOURNALROLEENUM中ROLE数组的序号一一对应
                Role r = JSONObject.toJavaObject(arr.getJSONObject(i), Role.class);
                roleList.add( new JournalRole( r ));
            }

            rst.put(jid, roleList.toArray(new JournalRole[0]));
        }

        return rst;
    }
}
