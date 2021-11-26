package edu.nuist.ojs.middle.datamonitor.datamonitormap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Configuration
@Component
@PropertySource(value = {"classpath:/application-listboard.yml"} )
@ConfigurationProperties("listboard")
@Data
public class MonitorMapper {
    private HashMap<String, HashMap<String, MonitorQuery>> catagorys;

    public JSONObject getSysConfigList(){
        JSONObject obj = new JSONObject();
        for(Entry<String, HashMap<String, MonitorQuery>> e: this.catagorys.entrySet()){ 
            JSONObject setting = new JSONObject(); 
            for(Entry<String, MonitorQuery> entry: e.getValue().entrySet()){
                setting.put(entry.getKey(), Boolean.valueOf(entry.getValue().isShowDefault()).toString());
            }
            obj.put(e.getKey(), setting);
        }

        return obj;
    }


    public HashMap<String, List<String[]>> getShowConfigList(){
        HashMap<String, List<String[]>> rst = new HashMap<>();
        for(Entry<String, HashMap<String, MonitorQuery>> e: this.catagorys.entrySet()){ 
            String cata = e.getKey();
            List<String[]> data = rst.get(cata);

            if( data == null ){
                data = new ArrayList<>();
            }
            
            List<Map.Entry<String, MonitorQuery>> list = new ArrayList<Map.Entry<String, MonitorQuery>>(e.getValue().entrySet()); 
            Collections.sort(list, new Comparator<Map.Entry<String, MonitorQuery>>() {
                @Override
                public int compare(Map.Entry<String, MonitorQuery> o1, Map.Entry<String, MonitorQuery> o2) {
                    return o1.getValue().getIndex() > (o2.getValue().getIndex()) ? 1 : -1;
                }
            });

            //System.out.println( cata + "=======-----------------------------------------" );

            for(Entry<String, MonitorQuery> en: list){
                //System.out.println( en.getKey() );
                data.add( new String[]{
                    en.getKey(), en.getValue().getTitle(), en.getValue().isShowDefault()?"true":"false", en.getValue().getIndex()+"", en.getValue().getSize()
                });
            }
            rst.put(cata, data);
        }
        return rst;
    }
}
