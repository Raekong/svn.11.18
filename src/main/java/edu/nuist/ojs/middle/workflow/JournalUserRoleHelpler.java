package edu.nuist.ojs.middle.workflow;

import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.journalsetting.params.JournalSection;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class JournalUserRoleHelpler {
    @Autowired
    private CallStub callStub;

    public List<User> getStageEditors(long jid, long rid){
        String rst = callStub.callStub("getJournalEditorTeamByRole", String.class, "jid", jid, "rid", rid); 
        List<User> users = new LinkedList<>(); 
        try{
            JSONArray obj = JSONObject.parseArray(rst); 
            for(int i=0; i<obj.size(); i++){
                User u = JSONObject.toJavaObject(obj.getJSONObject(i), User.class);
                users.add(u);
            }
            return users;
        }catch(Exception e) {
            
        }
        return null;
    }

    public User getDefaultStageEditor(long jid, long rid){
        String rst = callStub.callStub("getJournalEditorTeamByRole", String.class, "jid", jid, "rid", rid); 
        try{
            System.out.println(rst);
            JSONObject obj = JSONObject.parseArray(rst).getJSONObject(0);
            return JSONObject.toJavaObject(obj, User.class);
        }catch(Exception e) {
            e.printStackTrace();
            return getJournalManager( jid);
        }
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
    
    public boolean isSectionAuthority(long aid){
        JournalSection js = callStub.callStub("isSectionAuthorityByAid", JournalSection.class, "aid", aid); 
        return js.isAuthority();
    }

}
