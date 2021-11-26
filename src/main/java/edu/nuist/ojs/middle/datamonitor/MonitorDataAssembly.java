package edu.nuist.ojs.middle.datamonitor;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpSession;

public class MonitorDataAssembly {
    public static final String MONITOR_PARAMS = "monit_params";
    public static void assembly(HttpSession session, Object... params){
        JSONObject jsonObject=new JSONObject();
        for(int i=0;i<params.length;i+=2){
            jsonObject.put(params[i].toString(),params[i+1]);
        }

        session.setAttribute(MONITOR_PARAMS, jsonObject);
    }

    public static JSONObject assembly( Object... params){
        JSONObject jsonObject = new JSONObject();
        for(int i=0;i<params.length;i+=2){
            jsonObject.put(params[i].toString(),params[i+1]);
        }

        return jsonObject;
    }
}
