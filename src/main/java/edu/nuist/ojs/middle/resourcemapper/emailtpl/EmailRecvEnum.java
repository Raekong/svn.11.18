package edu.nuist.ojs.middle.resourcemapper.emailtpl;

import java.util.LinkedList;
import java.util.List;

public enum EmailRecvEnum {
    SUBMITOR("Submitor", "投稿人",  1),
    SUBMITORANDCORRESPOND("Submitor and Corresponding Author", "投稿人与通讯作者", 2),
    ALLAUTHORS("All Author", "所有作者", 3);

    private int index;
    private String en;
    private String zh;
    
    
    private EmailRecvEnum(String en, String zh,  int index) {
        this.en = en;
        this.zh = zh;
        this.index = index;
    }
    
    public static EmailRecvEnum getByIndex(int index) {  
        for (EmailRecvEnum c : EmailRecvEnum.values()) {  
            if (c.getIndex() == index) {  
                return c;
            }  
        }  
        return null;  
    }

    public int getIndex() {
        return index;
    }

    public String getEn() {
        return en;
    }

    public String getZh() {
        return zh;
    }
    
    public static List<EmailRecvEnum> getAll(){
    	List<EmailRecvEnum>  rst = new LinkedList<>();
    	for (EmailRecvEnum c : EmailRecvEnum.values()) {  
            rst.add(c);
        }  
        return rst;  
    } 
}
