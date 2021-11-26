package edu.nuist.ojs.middle.interceptor;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSONObject;
import edu.nuist.ojs.middle.resourcemapper.i18n.PageI18N;
import edu.nuist.ojs.middle.resourcemapper.modules.Modules;

@Component
public class I18NInterceptor {
    public static final String I18NFORPAGE = "i18nInfos";
    public static final String I18N = "i18n";

    @Autowired
    private PageI18N pageI18N;

    @Autowired
    private  Modules modules;
    
    public void exec(HttpServletRequest request, ModelAndView modelAndView, String configKeys){
        String[] keys = configKeys.split(",");
        //约定在MODEL里面先放上语种
        boolean i18n = (boolean)modelAndView.getModel().get(I18N); 
        HashMap<String, String > rst = new HashMap<>();
        for(String key : keys){
            HashMap<String, String> infos = pageI18N.getPageInfo(
                key.trim(), 
                i18n
            );
           
            if(infos != null)   rst.putAll(infos);
        }
        
        modelAndView.getModel().putAll( rst );//为页面显示准备
        modelAndView.getModel().put(I18NFORPAGE, JSONObject.toJSONString(rst));//为页面提示

        if(configKeys.indexOf("modules") != -1){ //为功能模块添加的额外
            modelAndView.getModel().put("modules", modules.getModules(i18n));
        }
    }
      

}