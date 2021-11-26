package edu.nuist.ojs.middle;


import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import edu.nuist.ojs.middle.resourcemapper.i18n.I18nInfo;
import edu.nuist.ojs.middle.resourcemapper.i18n.PageI18N;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

@Configuration
public class InitI18N {
    @Autowired
    private PageI18N pageI18N;

    @Value("${global.i18n}")
    private String xls;


    @PostConstruct
	public void initI18N() throws IOException {
		Resource resource = new ClassPathResource( xls );
        InputStream in = resource.getInputStream();
        ExcelReader reader = ExcelUtil.getReader( in );
       
        List<List<Object>> reads = reader.read();
        System.out.println(reads.size());
        HashMap<String, HashMap<String, I18nInfo>> pages = new HashMap<>();

        for(int i=0;i<reads.size();i++)
        {
            
            Object[] obj = reads.get(i).toArray();
            I18nInfo info = new I18nInfo();
            info.setZh(obj[2].toString());
            info.setEn(obj[3].toString());

            HashMap<String, I18nInfo> infoHashMap = pages.get(obj[0].toString());
            if( infoHashMap == null ){
                infoHashMap = new HashMap<>();
            }
            
            infoHashMap.put(obj[1].toString(), info );
            pages.put(obj[0].toString(), infoHashMap);
            
        }

        pageI18N.setPages(pages);
    }
}