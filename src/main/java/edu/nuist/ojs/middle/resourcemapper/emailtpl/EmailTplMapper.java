package edu.nuist.ojs.middle.resourcemapper.emailtpl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.EmailConfigPoint;
import edu.nuist.ojs.common.entity.EmailTpl;

import java.util.LinkedList;
import java.util.List;

@Configuration
@Component
@PropertySource(value = {"classpath:/application-emailtpl.yml"} )
@ConfigurationProperties("template")
@Data
public class EmailTplMapper {
    private List<EmailConfigPoint> systemEmailConfigPoints;

    
    public List<String> getAllSystemConfigPoint(){
        List<String> rst = new LinkedList<>();
        for(EmailConfigPoint ep : systemEmailConfigPoints){
            rst.add( ep.getConfigPoint() );
        }

        return rst;
    }

    public EmailTpl getSystemTpl(String point){
        for(EmailConfigPoint ep : systemEmailConfigPoints){
            if( ep.getConfigPoint().equals(point)){
                return ep.getTpls().get(0);
            }
        }
        return null;
    }

    public EmailConfigPoint getEmailConfigPoint(String point){
        for(EmailConfigPoint ep : systemEmailConfigPoints){
            if( ep.getConfigPoint().equals(point)){
                return ep;
            }
        }
        return null;
    }

}
