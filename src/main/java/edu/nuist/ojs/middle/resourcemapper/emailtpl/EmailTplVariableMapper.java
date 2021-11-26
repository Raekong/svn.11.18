package edu.nuist.ojs.middle.resourcemapper.emailtpl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Configuration
@Component
@PropertySource(value = {"classpath:/application-emailtpl.yml"} )
@ConfigurationProperties("template")
@Data
public class EmailTplVariableMapper {
    private HashMap<String , HashMap<String, String>> tplvariables;
}
