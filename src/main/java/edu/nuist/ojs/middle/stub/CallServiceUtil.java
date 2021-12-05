package edu.nuist.ojs.middle.stub;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;



@Service
public class CallServiceUtil {
    @Autowired
    RestTemplate restTemplate;

    @Value("${global.simialcheck.centerIp}")
    private String centerIp;

    public <T> JSONObject basicAssembly(String service,String api,T bean){
        JSONObject data=new JSONObject();
        data.put("service",service);
        data.put("api",api);
        data.put("data",JSONObject.toJSONString(bean));
        return data;
    }
    public <T> T send(JSONObject data,Class resultTpye,String serverRouter){
        String url=centerIp+"/"+serverRouter;
        return (T) restTemplate.postForObject(url,data,resultTpye);
    }
   
    public <T> T callService(String service, String api, Map<String,Object> data, Class<T> resultType, String serverRouter){
        JSONObject Data=basicAssembly(service,api,data);
        return send(Data,resultType,serverRouter);
    }


    public Map<String, Object> basicAssembly( String api, Object bean){
        Map<String, Object> data = new HashMap<>();
        data.put("api",api);
        data.put("data", JSONObject.toJSONString(bean, SerializerFeature.WriteMapNullValue) );
        return data;
    }

    public <T> T send(String router, Map<String, Object> data, Class<T> resultTpye){
        return (T) restTemplate.postForObject(router, data, resultTpye);
    }

    public <T> T callService(
        String server, 
        String router,
        String apiUrl, 
        Map<String, Object> data, 
        Class<T> resultType
    ){
        String url = server + router;
        Map<String, Object> param = basicAssembly( apiUrl, data);
        System.out.println( "=========-----------------------------------" + url );
        System.out.println( param );
        return send(url, param, resultType);
    }
}
