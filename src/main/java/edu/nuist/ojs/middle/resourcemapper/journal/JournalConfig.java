package edu.nuist.ojs.middle.resourcemapper.journal;

import java.util.LinkedList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import lombok.Data;

@Data
class Config{
    private String configPoint;
    private String data;

    public JournalSetting getJs(){
        return JournalSetting.builder().configContent(this.data.toString()).configPoint(this.configPoint).build();
    }
}

@Configuration
@Component
@PropertySource(value = {"classpath:/application-journalconfig.yml"} )
@ConfigurationProperties("journal")
@Data
public class JournalConfig {
    private List<Config> configs ;

    public JournalSetting getConfig(String key){
        for( Config c : configs){
            if( c.getConfigPoint().equals(key)){
                return c.getJs();
            }
        }
        return null;
    }

    public List<String> getConfigPoints(){
        List<String> keys = new LinkedList<>();
        for(Config c: configs){
            keys.add( c.getConfigPoint() );
        }
        return keys;
    }
}
