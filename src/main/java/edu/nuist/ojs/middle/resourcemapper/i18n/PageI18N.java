package edu.nuist.ojs.middle.resourcemapper.i18n;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map.Entry;

@Component
@Data
public class PageI18N {
    private HashMap<String, HashMap<String, I18nInfo>> pages = new HashMap<>();

    public HashMap<String, String> getPageInfo( String page,  boolean isZh ){
        if( pages.get(page ) != null){
            HashMap<String, String> rst = new HashMap<String, String>();
            for(Entry<String, I18nInfo> info: pages.get(page).entrySet()){
                rst.put( info.getKey(), isZh ? info.getValue().getZh(): info.getValue().getEn());
            }
            System.out.println(rst);
            return rst;
        }
        return null;
    }
}
