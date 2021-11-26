package edu.nuist.ojs.middle.journalsetting.params;

import edu.nuist.ojs.common.entity.journalsetting.JournalRole;
import edu.nuist.ojs.common.entity.journalsetting.JournalSettingParam;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.json.JSONArray;



@Data
public class JournalTeamSetting extends JournalSettingParam{
    private HashMap<JournalRole , List<JournalEditor>> teams = new HashMap<>() ; //角色JOURNALROLE 的JSONSTRING，对应某角色所有的用户EMAIL
    public JournalTeamSetting(){

    }
    public JournalTeamSetting(String jsonStr){
        JSONObject obj = JSONObject.parseObject(jsonStr).getJSONObject("teams");
        //System.out.println( obj.keySet() );
        for(Object key : obj.keySet()){
            Object teamStr = obj.get(key);

            JournalRole role = JSONObject.toJavaObject((JSONObject) key, JournalRole.class);
            List<JournalEditor> editors = JSONObject.parseArray( teamStr.toString(), JournalEditor.class);
            this.teams.put( role, editors );
        }
    }

    public void removeRole(JournalRole role){
        JournalRole tmp = null;
        for(Entry<JournalRole , List<JournalEditor>> e : teams.entrySet()){
            if(e.getKey().getAbbr().equals(role.getAbbr())){
                tmp = e.getKey();
                break;
            } 
        }
        teams.remove(tmp);
    }
    public List<JournalEditor> getEditorsByRole(JournalRole role){
        for(Entry<JournalRole , List<JournalEditor>> e : teams.entrySet()){
            if(e.getKey().getAbbr().equals(role.getAbbr())){
                return e.getValue();
            } 
        }
        return null;
    }

    public void addEditor(JournalRole role, JournalEditor e){
        List<JournalEditor> editors = teams.get(role) ;
        if( editors == null){
            editors = new LinkedList<>();
        }
        
        for(JournalEditor je : editors){
            if( je.getEmail().trim().equals(e.getEmail().trim()) ){
                return;
            }
        }

        editors.add(e);
        teams.put(role, editors);
        return;
    }
}
