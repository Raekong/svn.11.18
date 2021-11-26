package edu.nuist.ojs.middle.resourcemapper.journal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import cn.hutool.core.map.MapUtil;
import edu.nuist.ojs.common.entity.journalsetting.JournalRole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Data
class menu{
    private int id;
    private String zh;
    private String en;
    private String url;
    private String icon;
}

@Configuration
@Component
@PropertySource(value = {"classpath:/application-journalrolemenu.yml"} )
@ConfigurationProperties("journalrolemenu")
@Data
public class JournalRoleMenuSetting {
    private HashMap<String, String> rolemenus; //journal 配置的角色可以访问的菜单列表
    private List<menu> menus;    //每个角色可以显示的菜单列表
    private HashMap<String, String> jouranlroles; //journals可以配置的角色列表

    public Map<String, String> getMenusByRole( JournalRole role, boolean isZh ){
        HashMap<String, String> rst = new HashMap<> ();
        String menuStr = rolemenus.get(role.getSameAbbr() == null ? role.getAbbr() : role.getSameAbbr() );
        for(String menuId : menuStr.split(",")){
            for(menu m : menus){
                if( m.getId() == Integer.valueOf(menuId)){
                    rst.put( isZh? m.getZh() : m.getEn(), m.getId()+ ";"+m.getUrl() +";"+ m.getIcon() );
                }
            }
        }
        return MapUtil.sortByValue(rst, false);
    }

    public Map<String, String> getMenusForSuper(  boolean isZh ){
        HashMap<String, String> rst = new HashMap<> ();
        String menuStr = rolemenus.get("super" );
        for(String menuId : menuStr.split(",")){
            for(menu m : menus){
                if( m.getId() == Integer.valueOf(menuId)){
                    rst.put( isZh? m.getZh() : m.getEn(), m.getId()+ ";"+m.getUrl() +";"+ m.getIcon() );
                }
            }
        }
        return MapUtil.sortByValue(rst, false);
    }

    public Map<String, String> getMenusForRoot(  boolean isZh ){
        HashMap<String, String> rst = new HashMap<> ();
        String menuStr = rolemenus.get("root" );
        for(String menuId : menuStr.split(",")){
            for(menu m : menus){
                if( m.getId() == Integer.valueOf(menuId)){
                    rst.put( isZh? m.getZh() : m.getEn(), m.getId()+ ";"+m.getUrl() +";"+ m.getIcon() );
                }
            }
        }
        return MapUtil.sortByValue(rst, false);
    }
    
}
