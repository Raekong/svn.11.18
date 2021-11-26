package edu.nuist.ojs.middle.resourcemapper.article;

import lombok.Data;

@Data
public class ArticleStatus {
    private String statusZH;
    private String statusEN;
    private String descZH;
    private String descEN;
    private int index;

    public String getStatus(boolean isZH){
        String t = isZH? this.statusZH : this.statusEN;
        if( t == null ){
            t =  isZH? this.statusEN  : this.statusZH ;
        }
        return t;
    }

    public String getDesc(boolean isZH){
        String t = isZH? this.descZH : this.descEN;
        if( t == null ){
            t =  isZH? this.descEN  : this.descZH ;
        }
        return t;
    }
}
