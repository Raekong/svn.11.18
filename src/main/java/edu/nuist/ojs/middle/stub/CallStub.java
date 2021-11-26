package edu.nuist.ojs.middle.stub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import edu.nuist.ojs.common.entity.EmailFile;
import edu.nuist.ojs.common.entity.EmailServer;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.middle.redis.RedisRouter;
import edu.nuist.ojs.middle.resourcemapper.stub.ServiceMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.User;

import cn.hutool.crypto.SecureUtil;
import java.util.UUID;

@Component
public class CallStub {
    @Autowired
    private CallServiceUtil callUtil;

    @Autowired
    private ServiceMap seviceMap;

    @Autowired
    RedisRouter redisRouter;
    
    @Autowired
    RedisTemplate<Object,Object> redisTemplate;

    public <T> T callStub(String func, Class<T> result,Object... params){
       Map<String,Object> param=new HashMap<String, Object>();
       for(int i=0;i<params.length;i+=2){
           param.put(params[i].toString(), params[i+1]);
       }
       System.out.println( param );
       String[] tmp = seviceMap.get(func);
       if( tmp != null) {
            return callUtil.callService(tmp[0], tmp[1], tmp[2], param, result);
       }else{
            System.out.println("CALL STUB ERROR----------------================");
            return null;
       }
    }

    public JSONObject createJornal(String abbr, String name, String email, long pid){
        Map<String, Object> param = new HashMap<>();
        param.put("abbr", abbr);
        param.put("name", name);
        param.put("email", email);
        param.put("pid", pid);
        return callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/journal/create", param, JSONObject.class);
        //redisTemplate.opsForValue().set(p.get(new Object()), p);
    }

    public  Publisher findPublisherByAbbr(String abbr){
        Map<String, Object> param = new HashMap<>();
        param.put("abbr", abbr);
        Publisher p = callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/publish/findByAbbr", param, Publisher.class);
        //redisTemplate.opsForValue().set(p.get(new Object()), p);
        return p;
    }

    public  User login(String email, String password, long publishId){
        Map<String, Object> param = new HashMap<>();
        param.put("email", email);
        param.put("password", password);
        param.put("publishId", publishId);
        User u = callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/user/login", param, User.class);
        return u;
    }

    public User findByPublisherIdAndEmail(long publisherId, String email){
        Map<String, Object> param = new HashMap<>();
        param.put("publisherId", publisherId);
        param.put("email", email);
        User u = callUtil.callService(
            seviceMap.getList().get("publisher").getIp(),  
            "/serverRouter", "/user/findbyemailandpid", param, User.class);
        return u;
    }


    public  User setUsreI18N(long uid, String lang){//重置用户国际化
        Map<String, Object> param = new HashMap<>();
        param.put("uid", uid);
        param.put("lang", lang);
        User u = callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/user/setI18n", param, User.class);
        return u;
    }

    public User regist( User u ){
        Map<String, Object> param = new HashMap<>();
        param.put("u", u);
        return callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/user/regist",  param, User.class);
    }

    public String isSuper( String email ){
        Map<String, Object> param = new HashMap<>();
        param.put("email", email);
        return  callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/user/isSuper", param , String.class);
    }

    public Map<String, Object> resetPassword(String publisher,String email){
        Map<String, Object> param = new HashMap<>();
        param.put("email", email);
        param.put("publisherAbbr", publisher);
        return  callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/user/resetpassword", param , Map.class);
    }



    public String getPublisherList(String name, String abbr, int pageNum, int pageSize){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("name", name);
        param.put("abbr", abbr);
        param.put("page", pageNum);
        param.put("size",pageSize);
        return   callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/publish/getAllPagePublishers", param , String.class);
    }

    public Publisher updatePublisher(Publisher publisher){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("publisher",publisher);
        return  callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/publish/update", param , Publisher.class);
    }

    public String searchPublishers(int pageNum, int pageSize, @Nullable String name, @Nullable String abbr)
    {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("page", pageNum);
        param.put("size",pageSize);
        param.put("name",name);
        param.put("abbr",abbr);
        return   callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/publish/searchPagePublishers", param , String.class);
    }

    public Publisher newPublisher(Publisher publisher, User user){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("publisher", publisher);
        param.put("user",user);
        return   callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/publish/regist",  param , Publisher.class);
    }
    public Publisher findPublisherById(long id){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("id",id);
        return  callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/publisher/findById", param , Publisher.class);
    }
    public Publisher publisherDisable(long id){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("pId",id);
        return  callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/publish/disable", param , Publisher.class);
    }
    public Publisher publisherEnable(long id){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("pId",id);
        return  callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/publish/enable", param , Publisher.class);
    }

    public String getPath(String id){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("key",id);
        return  callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/message/getPath", param , String.class);
    }
   
    public String getLocal(String id){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("random",id);
        return callUtil.callService(
            seviceMap.getList().get("publisher").getIp(), 
            "/serverRouter", "/message/recv",param,String.class);
    }

   
    public Message sendMessage(Message message){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("message",message);
        return  callUtil.callService(
            seviceMap.getList().get("email").getIp(), 
            seviceMap.getList().get("email").getRouter(), 
            "/message/send", param , Message.class);
    }

    public Message getMessage(long recvId, String configPoint){
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("id",recvId);
        param.put("configPoint",configPoint);
        return  callUtil.callService(
            seviceMap.getList().get("email").getIp(), 
            seviceMap.getList().get("email").getRouter(), 
            "/message/getMessageByRevIdAndConfigPoint", param , Message.class);
    }
}
