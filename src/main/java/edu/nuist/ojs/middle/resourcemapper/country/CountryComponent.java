package edu.nuist.ojs.middle.resourcemapper.country;

import edu.nuist.ojs.middle.file.FileUtil;
import edu.nuist.ojs.middle.file.LineProcess;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

class countryLp extends LineProcess {
    public List<Country> countries = new LinkedList<>();

    @Override
    public void doLine(String line) {
        //System.out.println(line);
        String[] tmp = line.split(";");
        countries.add(Country.builder().en(tmp[0]).zh(tmp[1]).code(tmp[2]).build());
    }

}

@Component
@Data
public class CountryComponent {

    private CountryList list = null;

    //加载国家列表,用于用户注册
    @PostConstruct
	public void initCountries(  ) throws Exception {
        Resource resource = new ClassPathResource("countries.txt");

        countryLp lp = new countryLp();
        FileUtil.processInLine(new InputStreamReader(resource.getInputStream()), lp);
        
        list = new CountryList();
        
        list.setCityList(lp.countries);
	}

    public TreeMap<String, String> listCountries( boolean isZh ) throws Exception {
        return list.getCityList( isZh );

    }
    
}
