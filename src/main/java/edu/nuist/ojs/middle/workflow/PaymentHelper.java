package edu.nuist.ojs.middle.workflow;

import java.util.HashMap;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.Link;
import edu.nuist.ojs.common.entity.Payment;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class PaymentHelper {
    @Autowired
    private CallStub callStub;

    @Value("${global.linkserver}")
    private String linkserver;
    
    public String render(String content, HashMap<String, String> model){ 
        String tmp = content;
        for(Entry<String, String> entry : model.entrySet()) {
            String key = entry.getKey();
            while( tmp.indexOf( key ) != -1  ) {
                String pre =  tmp.substring(0, tmp.indexOf( key ));
                String post = tmp.substring(tmp.indexOf(key)+ key.length());
                tmp = pre + entry.getValue() + post;
            }
        }
        return tmp;
    }

    public Payment getPaymentById(long payid){
        return callStub.callStub("getPaymentById", Payment.class,"payid", payid);
    }

    public Payment save(Payment pay){
        return  callStub.callStub("savePayment", Payment.class, "payment", pay);     
    }



    public JSONObject getAPCInfos(long jid, int pageNum){
        HashMap<String, String> model = new HashMap<>();
        /**
         * #Article Processing Charge# 
            #Over Charge#
            #Basic Pages#
            #Total APC#
            $#Wrie Transfer#
            $#Total Amount Pay by Wire Transfer#
            #Online Transfer Fee# 
            #Total Amount Pay by online#
         */

        JournalSetting payment = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid,"configPoint", JournalConfigPointEnum.Payment);
        JournalSetting basicnum = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid,"configPoint", JournalConfigPointEnum.Basic_pages_Number);
        JournalSetting overcharge = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid,"configPoint", JournalConfigPointEnum.Over_Charge);
        JournalSetting online = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid,"configPoint", JournalConfigPointEnum.Online_Transfer_Fee);
        JournalSetting wire = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid,"configPoint", JournalConfigPointEnum.Wire_Transfer);
        JournalSetting apc = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid,"configPoint", JournalConfigPointEnum.Article_Processing_Charge);

        if( payment.getConfigContent().equals("false")){
            return null;
        }

        double basicCharge = Double.valueOf(apc.getConfigContent());
        double overCharge = Double.valueOf(overcharge.getConfigContent());
        double wirefeePre = Double.valueOf(wire.getConfigContent());
        double onlinefeePre = Double.valueOf(online.getConfigContent());

        int overPage = pageNum - Integer.valueOf(basicnum.getConfigContent());


        int totalAPC = (int)( overPage > 0  ? overPage*overCharge +  basicCharge :  basicCharge ) ;
        int wirefee = (int)( totalAPC * ( wirefeePre*1.0 /100));
        int totalWire = (int)(totalAPC * (1 + wirefeePre*1.0 /100 )) ;
        int onlinefee = (int)( totalAPC * ( onlinefeePre*1.0 /100));
        int totalOnline = (int)(totalAPC * (1 + onlinefeePre*1.0 /100 )) ;
        //支付链接
        Link l = Link.builder().build();
        String md5 = l.getMD5();

        JSONObject  obj = new JSONObject(); 
        obj.put("totalAPC", totalAPC); 
        obj.put("totalWire", totalWire); 
        obj.put("totalOnline", totalOnline); 
        obj.put("onlinefee", onlinefee);
        obj.put("wirefee", wirefee);
        obj.put("linkmd5", md5);

        model.put("#Article Processing Charge#", apc.getConfigContent());
        model.put("#Over Charge#", overcharge.getConfigContent());
        model.put("#Basic Pages#", basicnum.getConfigContent());
        model.put("#Total APC#", Integer.valueOf((int)totalAPC).toString());
        model.put("#Wrie Transfer#", Integer.valueOf((int)(totalAPC * (wirefee*1.0 /100) )).toString()); 
        model.put("#Total Amount Pay by Wire Transfer#", Integer.valueOf((int)totalWire).toString()); 
        model.put("#Online Transfer Fee# ", Integer.valueOf((int)(totalAPC * (onlinefee*1.0 /100) )).toString()); 
        model.put("#Total Amount Pay by online#", Integer.valueOf((int)totalOnline).toString()); 
        model.put("#Payment Link#", linkserver + md5); 
        JournalSetting apcinfo = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid,"configPoint", JournalConfigPointEnum.About_Article_Processing_Charge);
        
        obj.put("apcinfo", render(apcinfo.getConfigContent(),  model));


        return obj;

    }
}
