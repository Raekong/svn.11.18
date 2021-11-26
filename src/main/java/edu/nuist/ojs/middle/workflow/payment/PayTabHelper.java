package edu.nuist.ojs.middle.workflow.payment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class PayTabHelper {
    @Autowired
    private CallStub callStub;
    
    @Autowired
    private JournalSettingHelper jshHelper; 

    public String getBankInfo(long jid){
        JournalSetting js  = callStub.callStub("getSetting",JournalSetting.class,"journalId",jid,"configPoint", JournalConfigPointEnum.Bank_Info);
        if( js == null ){
            return "";
        }
        return js.getConfigContent();
    }
    
    public JSONObject getPaymentsByAid(long aid){
        String json = callStub.callStub("getPaymentsByAid", String.class, "aid", aid);
        JSONArray pays = JSONArray.parseArray(json);
        JSONObject rst = new JSONObject();
        JSONArray userPays = new JSONArray();
        int total = 0;
        rst.put("audit", false);
        boolean audit = false;
        for(int i=0; i<pays.size(); i++){
            JSONObject obj = pays.getJSONObject(i);
            if( !obj.getBoolean("back") ){
                rst.put("origin", obj);
            }

            if( obj.getIntValue("state") == 2 ){
                audit = true;
            }

            total += obj.getIntValue("payTotal");
            userPays.add(obj);
        } 
        rst.put("total", total);
        rst.put("pays", userPays);
        rst.put("audit", audit);
        return rst;
    }
}
