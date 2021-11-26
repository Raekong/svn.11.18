package edu.nuist.ojs.middle.resourcemapper.journal;

import java.util.LinkedList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.article.Article;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import lombok.Data;

@Configuration
@Component
@PropertySource(value = {"classpath:/application-journalconfig.yml"} )
@ConfigurationProperties("journal")
@Data
public class JorunalFileType {
    private List<String> filetypes ;

    public ArticleFile getManscriptInWord(List<ArticleFile> files){
        ArticleFile af = null;
        for(ArticleFile f : files){
            if(  f.getFileType().equals("Manuscript in Word")){
                return  f;
            }
        }
        return af;
    }

    public ArticleFile getManscriptInPdf(List<ArticleFile> files){
        ArticleFile af = null;
        for(ArticleFile f : files){
            if(  f.getFileType().equals("Manuscript in Pdf")){
                return  f;
            }
        }
        return af;
    }

}
