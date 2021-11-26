package edu.nuist.ojs.middle.context;

import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.journalsetting.JournalRole;
import edu.nuist.ojs.common.entity.journalsetting.JournalRoleEnum;
import edu.nuist.ojs.middle.journalsetting.JournalRoleCharge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Context {
    private User user;
    private HashMap<Long,  JournalRole[]>  roles; //期刊ID， 后面的应该是角色
    private Publisher publisher;      
    private HashMap<String, Object> something;
   
    public Context( 
        User u, 
        Publisher p, 
        HashMap<Long,  JournalRole[]> roles  //用户对应所有期刊的角色, 以期刊ID号KEY，所在期刊用户担任的所有角色
    ){
        this.user = u;
        this.publisher = p;
        this.roles = roles;
    }
    
    //根据访问权限的层次来获得角色
    //ROLE这部分的设计非常复杂，问题主要在于ROLE可以自定义，但是MANAGER又限定只能有一个人
    //自定义的角色要设定成与系统角色相同的某一个角色
    //因此判断来带来问题
    /**
     * 在系统中，通过角色的ABBR属性来实现的，自定义和系统的角色都是ABBR这一个属性，设置自定义角色的时间用户可以
     * 设定，此时进入库中有一个属性 samelevel来指出与系统角色相同的属性序号，从库中出来后，由COMMON模块将其转成JOURNAL ROLE
     * 的时候，根据这个属性设置库中的自定义ROLE的ABBR，所有的权限判断都是基于这个ABBR属性是否相等来实现的
     * 
     * 判断权限的时候，系统分成三层
     * 高层:MANAGER一个层
     * 中层：SECTION 一个层
     * 低层：AUTHOR 和REVIEWER一个层
     * 
     * @return
     */
    public JournalRole getRoleByChargeLevel(){
        int min = JournalRoleCharge.roles.length - 1 ; //最低的AUTHOR, REVIEWER
        
        for(JournalRole[] tmp : roles.values()){
            JournalRole role = JournalRoleCharge.getRoleByChargeLevel(Arrays.asList(tmp));
            if( min > JournalRoleCharge.getRoleIndex(role)){
                min = JournalRoleCharge.getRoleIndex(role);
            }
        }
        return JournalRoleCharge.getRoleByLevel(min);
    }

    //根据当前用户的权限列表，获取其担任主管的期刊ID列表
    public List<Long> getManager(){
        List<Long> rsts = new LinkedList<Long>();
        
        for(Entry<Long, JournalRole[]> e : roles.entrySet()){
           
            for(JournalRole r: e.getValue()){
                if( r.getAbbr().equals(JournalRoleEnum.MANAGER.getAbbr()) ){ //使用隐藏的属性ABBR来判断
                    rsts.add( e.getKey() );
                }
            }
        }
        return rsts;
    }


    public static Map<String, Object> objectToMap(Object obj) {
		if (obj == null) {  return null; }

		Map<String, Object> map = new HashMap<String, Object>();
		try {
			Field[] declaredFields = obj.getClass().getDeclaredFields();
			for (Field field : declaredFields) {
				field.setAccessible(true);
				map.put(field.getName(), field.get(obj));
			}
		} catch (Exception e) {}

		return map;
	}

    public Object get(String key){
        String[] k = key.split("[.]");

        Object t = null;
        //先取出版社配置，再取额外的零星配置，再取用户自己的配置，最终叠加一起
        if(k[0].toLowerCase().trim().equals("p")){//publisher
            t = this.publisher; 
        }else if(k[0].toLowerCase().trim().equals("s")){//somethng,如果是直接返回
            t = this.something;
            return this.something.get(k[1]);
        }else if(k[0].toLowerCase().trim().equals("u")){//user
            t = this.user;
        }
        if( t != null ){
            return objectToMap(t).get(k[1]);
        }
        return null;
        
    }

    //从Context中取出某个配置项,先从用户配置型中抽取，没有则从publisher中抽取
    //按道理还应从journalSetting中抽取，但是这块子和某个人的Context配置无关，所有还是应该从JOURANL SETTING中直接去拿
    public <T>  Object  getSetting(String key){
        String rst = null;
        rst = this.user.get(key);
        return rst; 
        
    }
}
