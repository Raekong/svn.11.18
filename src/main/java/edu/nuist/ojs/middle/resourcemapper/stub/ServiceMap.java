package edu.nuist.ojs.middle.resourcemapper.stub;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Configuration
@Component
@PropertySource(value = {"classpath:/application-services.yml"} ) 
@ConfigurationProperties("nacos")
@Data
public class ServiceMap{
    private HashMap<String, ServicePoint> list = new HashMap<>();

    public HashMap<String, ServicePoint> getList(){
        return this.list;
    }

    public String[] get(String service){
        String[] tmp = null;
        for(Entry<String, ServicePoint> e: list.entrySet()){
            List<String> points = e.getValue().isMatch(service);
            if( points.size() > 1 ) {
                System.out.println("same stub point error======------------------");
                return null;
            }else if(points.size() > 0){
                tmp = new String[3];
                tmp[0] = e.getValue().getIp();
                tmp[1] = e.getValue().getRouter();
                tmp[2] = points.get(0);
            }
        }
        return tmp;
    }

    public String getUrl(String service ){
        String endpoint = list.get(service).getIp();
        String router = list.get(service).getRouter();
        return "http://" + endpoint + router;
    }
    
}
