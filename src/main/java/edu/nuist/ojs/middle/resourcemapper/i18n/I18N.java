package edu.nuist.ojs.middle.resourcemapper.i18n;

import java.util.HashMap;
import java.util.Map.Entry;

public class I18N {
    public static final String EN = "en";
    public static final String CN = "zh";
    public HashMap<String, String> exec( boolean isZh ){
        HashMap<String, String> rst = new HashMap<String, String>();
        for(Entry<String, I18nInfo> info: getInfos().entrySet()){
            rst.put( info.getKey(), isZh ? info.getValue().getZh(): info.getValue().getEn());
        }
        return rst;
    }

    public HashMap<String, I18nInfo> getInfos(){
        return null;
    }
}
