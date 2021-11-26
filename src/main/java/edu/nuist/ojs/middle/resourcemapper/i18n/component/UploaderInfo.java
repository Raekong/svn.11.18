package edu.nuist.ojs.middle.resourcemapper.i18n.component;


import edu.nuist.ojs.middle.resourcemapper.i18n.I18N;
import edu.nuist.ojs.middle.resourcemapper.i18n.I18nInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Configuration
@Component
@PropertySource(value = {"classpath:/application-i18n.yml"} )
@ConfigurationProperties("i18n")
@Data
@EqualsAndHashCode(callSuper=false)
public class UploaderInfo extends I18N {
    private HashMap<String, I18nInfo> uploader = new HashMap<String, I18nInfo>();

    public HashMap<String, I18nInfo> getInfos(){
        return this.uploader;
    }
}
