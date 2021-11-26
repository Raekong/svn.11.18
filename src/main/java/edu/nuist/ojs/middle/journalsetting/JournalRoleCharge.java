package edu.nuist.ojs.middle.journalsetting;

import java.util.List;

import edu.nuist.ojs.common.entity.journalsetting.JournalRole;
import edu.nuist.ojs.common.entity.journalsetting.JournalRoleEnum;

public class JournalRoleCharge {
    //这里定义的角色与APPLICATION-journalrolemenu.yml中配置的角色是一一对应的
    //权限分三级，最高级是MANAGER， 仅之是CHIEF,OFFICE
    //其它除倒数第一级，都是各业务流环节承担的角色
    //最后一级最低
    public static JournalRole[][] roles = {
        { JournalRoleEnum.MANAGER, JournalRoleEnum.CHIEF, JournalRoleEnum.OFFICE},
        { JournalRoleEnum.SECTION, JournalRoleEnum.GUEST, JournalRoleEnum.ACHIEF },
        { JournalRoleEnum.FINANCIAL },
        { JournalRoleEnum.PRODUCTION },
        { JournalRoleEnum.COPY },
        { JournalRoleEnum.SIMILARITY },
        { JournalRoleEnum.AUTHOR, JournalRoleEnum.REVIEWER}
    };

    //根据用户权限来决定显示的菜单项，统共分三级，最高一层级，中间编辑层级，最低普通用户层级
    public static JournalRole getRoleByChargeLevel(List<JournalRole> roles){
        int min = Integer.MAX_VALUE;
        for(JournalRole role: roles){
            int rowIndex = JournalRoleCharge.getRoleIndex(role);
            if( min> rowIndex) min = rowIndex;
        }
        return JournalRoleCharge.getRoleByLevel(min);
    }


    public static JournalRole getRoleByLevel( int rowIndex  ){
        if( rowIndex < 1){
            return  JournalRoleEnum.MANAGER;
        }

        if( rowIndex > 5){
            return JournalRoleEnum.AUTHOR;
        }

        return JournalRoleEnum.SECTION;
    }

    public static int getRoleIndex(JournalRole r){
        int index = 0;
        for(JournalRole[] role : roles ){ //系统定义的
            for(JournalRole tmp : role){  //系统定义与传入的角色通过ABBR判断
                if( tmp.getAbbr().equals(r.getAbbr()) ){
                    return index;
                }
            }
            index++;
        }
        return index;
    }

    public static JournalRole menu(JournalRole r){
        
        for(JournalRole[] role : roles ){
            for(JournalRole tmp : role){
                if( tmp.equals(r) ){
                    return role[0];
                }
            }
        }

        return null;
    }
}
