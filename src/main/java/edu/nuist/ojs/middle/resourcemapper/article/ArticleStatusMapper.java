package edu.nuist.ojs.middle.resourcemapper.article;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Configuration
@Component
@PropertySource(value = {"classpath:/application-articlestatus.yml"} )
@ConfigurationProperties("article")
@Data
public class ArticleStatusMapper{
    private HashMap<String , ArticleStatus> status; 
    private HashMap<String , String> workflow;  

    public ArticleStatus get(String key ){
        return status.get(key);
    }

    public String getWorkFlow(String key, boolean isZH){
        return workflow.get(key).split(";")[isZH?0:1];
    }

    public ArticleStatus getByIndex(int index){
        
        for(Entry<String , ArticleStatus> e : status.entrySet()){
            if( e.getValue().getIndex() == index){
                return e.getValue();
            }
        }

        return null;
    }
}