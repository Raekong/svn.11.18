package edu.nuist.ojs.middle.resourcemapper.modules;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.codec.digest.Md5Crypt;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Module{
    private String name;
    private String title;
    private String desc;

    public JSONObject get(boolean isZh){
        JSONObject obj = new JSONObject();
        obj.put("server", name);
        obj.put("title", isZh? title.split(";")[0]: title.split(";")[1]);
        obj.put("desc", isZh? desc.split(";")[0]: desc.split(";")[1]);
        return obj;
    }
}


@Configuration
@Component
@PropertySource(value = {"classpath:/application-journalrolemenu.yml"} )
@ConfigurationProperties("journalrolemenu")
@Data
public class Modules {
    private HashMap<String, Module> modules; //journal 配置的角色列表

    public List<JSONObject> getModules( boolean isZh ){
        List<JSONObject> rst = new LinkedList<>(); 
        for(Entry<String, Module> module: modules.entrySet()){
            module.getValue().setName(module.getKey());
            rst.add( module.getValue().get(isZh));
        }
        return rst;
    }
}

