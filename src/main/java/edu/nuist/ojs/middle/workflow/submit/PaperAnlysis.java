package edu.nuist.ojs.middle.workflow.submit;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSONObject;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

@Component
public class PaperAnlysis {
    private String getFileContent(String fileUrl) throws IOException, TikaException{
        URL newUrl =new URL(fileUrl);
        Tika tika = new Tika();
        String s = tika.parseToString(newUrl); 
        return s;
    }

    public boolean coverCheck(String fileUrl) throws IOException, TikaException{
        String s = getFileContent( fileUrl) ;
        if( s.toLowerCase().indexOf("apc")!=-1 
            || s.toLowerCase().indexOf("article processing charge")!=-1
        	|| s.toLowerCase().indexOf("$")!=-1
        ) {
        	return true;
        }else {
        	return false;
        }
    }

    public JSONObject getArticleParams(String fileUrl,  String fileType) throws IOException, TikaException {
        long old = System.currentTimeMillis();
       
        String s = getFileContent( fileUrl ); //抽取文档内容
        String[] s_blank = s.split("\n");

        if(fileType.indexOf("pdf")!=-1) {
            s_blank = s.split("\n\n");
        }

        String article_title = "";
        String article_author = "";
        String article_CorrespondingAuthor = "";
        String article_CorrespondingAuthor_name = "";
        String article_CorrespondingAuthor_email = "";
        int article_title_num = 0;
        int article_author_num = 0;
        int article_CorrespondingAuthor_num = 0;
        String s_blank_i = "";

        String regEx="([A-Za-z]+(\\s[A-Za-z]+\\W)*(\\d|\\*)+$)|(^(\\d|\\*)(\\s)*[A-Za-z]+(\\s[A-Za-z]+\\W)*)";
        Pattern p= Pattern.compile(regEx);//定义一个regEx模板
        Matcher m;
        boolean result;

        ok:
        for(int i=0;i<s_blank.length;i++) {
            s_blank_i=s_blank[i].replace("and", ",");
            if(s_blank_i.indexOf(",")==-1) {
                m=p.matcher(s_blank[i].trim());
                result=m.find();
                if(result) {
                    article_author_num = i;
                    article_title_num = i-1;
                    break ok;
                }
            }else if(s_blank_i.indexOf(",")!=-1) {
                String[] s_comma = s_blank_i.split(",");
                for(int j=0;j<s_comma.length;j++) {
                    m=p.matcher(s_comma[j].trim());//.trim删除头尾空白
                    result=m.find();
                    if(result) {
                        article_author_num = i;
                        article_title_num = i-1;
                        break ok;
                    }
                }
            }
        }

        for(int i=article_title_num;i>=0;i--) {
            if(s_blank[i]!= null && !s_blank[i].equals("")&& !s_blank[i].equals(" ")) {
                article_title = s_blank[i].trim();
                break;
            }
        }
        JSONObject jsonObject = new JSONObject();
//		HashMap<String, List<String>> hashMap = new HashMap<>();
//		List<String> list1 = new ArrayList<>();
        System.out.println("-------------article_title--------------");
        System.out.println(article_title);
        jsonObject.put("article_title",article_title);
//		list1.add(article_title);
//		hashMap.put("articleTitle",list1);

        article_author = s_blank[article_author_num];
        String pat = "\\d+" ;
        Pattern pa = Pattern.compile(pat) ;
        String author_digit[] = pa.split(article_author);
        List<String> author_name=new ArrayList<String>();
        List<String> author_num=new ArrayList<String>();
        for(int x=0;x<author_digit.length;x++){
            author_digit[x] = author_digit[x].replace(",","");
            author_digit[x] = author_digit[x].replace("，","");
            author_digit[x] = author_digit[x].replace("and","");
            author_digit[x] = author_digit[x].replace("*","");
            author_digit[x] = author_digit[x].replace("∗","");
            author_digit[x] = author_digit[x].replace("#","");
            if(author_digit[x].trim().equals("")) {
                continue;
            }
            author_name.add(author_digit[x].trim());
        }

        String article_author1 = article_author.replaceAll("[\\s,*]","");
        article_author1 = article_author1.replaceAll("[^0-9]","|");
        String[] article_author2 = article_author1.split("\\|");
        for(int i=0;i<article_author2.length;i++) {
            if(article_author2[i]!= null && !article_author2[i].equals("")&& !article_author2[i].equals(" ")) {
                author_num.add(article_author2[i].trim());
            }
        }
        System.out.println("================author_name======================");
//        List<String> list2 = new ArrayList<>();
        ArrayList<String> nameList = new ArrayList<>();
        for (int i = 0; i < author_name.size(); i++) {
            System.out.println(author_name.get(i));
            nameList.add(author_name.get(i));
//            list2.add(author_name.get(i));
        }
        jsonObject.put("author_name",nameList);
//        hashMap.put("authorName",list2);
//		System.out.print(hashMap);


        Pattern pattern = Pattern.compile("[a-zA-Z]");
        Matcher matcher;

        for(int i=0;i<s_blank.length;i++) {
            if(s_blank[i].toLowerCase().indexOf("correspond")!=-1) {
                article_CorrespondingAuthor_num = i;
                break;
            }else {
                article_CorrespondingAuthor_num = -1;
            }
        }
        try {
            article_CorrespondingAuthor = s_blank[article_CorrespondingAuthor_num];
            int corresponding_email = 0;
            if(article_CorrespondingAuthor.toLowerCase().indexOf("email")!=-1) {
                corresponding_email = article_CorrespondingAuthor.toLowerCase().indexOf("email");
                article_CorrespondingAuthor_email = article_CorrespondingAuthor.substring(corresponding_email+6).trim();
            }else if(article_CorrespondingAuthor.toLowerCase().indexOf("e-mail")!=-1) {
                corresponding_email = article_CorrespondingAuthor.toLowerCase().indexOf("e-mail");
                article_CorrespondingAuthor_email = article_CorrespondingAuthor.substring(corresponding_email+7).trim();
            }
            if(article_CorrespondingAuthor_email.indexOf(":")!=-1) {
                article_CorrespondingAuthor_email = article_CorrespondingAuthor_email.substring(article_CorrespondingAuthor_email.indexOf(":")+1).trim();
            }
            article_CorrespondingAuthor_name = article_CorrespondingAuthor.substring(article_CorrespondingAuthor.indexOf(":")+1,
                    corresponding_email).trim();
        }catch(Exception e) {
            System.out.println(e);
        }

        String Department_num;
        String Department_name;
        Map<String, String> Department_map=new HashMap<String, String>();
        if(fileType.indexOf("pdf")!=-1) {
            String[] s_blank_pdf;
            for(int i=article_author_num+1;i<=article_CorrespondingAuthor_num;i++) {
                s_blank_pdf = s_blank[i].split("\n");
                for(int k=0;k<s_blank_pdf.length;k++) {
                    matcher = pattern.matcher(s_blank_pdf[k]);
                    if (matcher.find()) {
                        Department_num = s_blank_pdf[k].substring(0,s_blank_pdf[k].indexOf(matcher.group())).trim();
                        Department_name = s_blank_pdf[k].substring(s_blank_pdf[k].indexOf(matcher.group())).trim();
                        String[] Department_num_list = Department_num.split(",");
                        if(Department_num_list.length>1) {
                            for(int j=0;j<Department_num_list.length;j++) {
                                Department_map.put(Department_num_list[j].trim(), Department_name);
                            }
                        }else if(Department_num.equals("")) {
                            matcher = pattern.matcher(s_blank_pdf[k-1]);
                            if(matcher.find()) {
                                System.out.println(s_blank_pdf[k-1].substring(s_blank_pdf[k-1].indexOf(matcher.group()))+" ");
                                Department_map.put(s_blank_pdf[k-1].substring(0,s_blank_pdf[k-1].indexOf(matcher.group())).trim(),
                                        s_blank_pdf[k-1].substring(s_blank_pdf[k-1].indexOf(matcher.group()))+" "+Department_name);
                            }
                        }else if(Department_num_list.length==1&&Department_num_list[0].length()>0) {
                            for(int j=0;j<Department_num_list[0].trim().length();j++) {
                                Department_map.put(String.valueOf(Department_num_list[0].trim().charAt(j)), Department_name);
                            }
                        }else{
                            Department_map.put(Department_num.trim(), Department_name);
                        }
                    }
                }
            }
        }else {
            for(int i=article_author_num+1;i<article_CorrespondingAuthor_num;i++) {
                matcher = pattern.matcher(s_blank[i]);
                if (matcher.find()) {
                    Department_num = s_blank[i].substring(0,s_blank[i].indexOf(matcher.group())).trim();
                    Department_name = s_blank[i].substring(s_blank[i].indexOf(matcher.group())).trim();
                    String[] Department_num_list = Department_num.split(",");
                    if(Department_num_list.length>1) {
                        for(int j=0;j<Department_num_list.length;j++) {
                            Department_map.put(Department_num_list[j].trim(), Department_name);
                        }
                    }else if(Department_num.equals("")) {
                        matcher = pattern.matcher(s_blank[i-1]);
                        if(matcher.find()) {
                            System.out.println(s_blank[i-1].substring(s_blank[i-1].indexOf(matcher.group()))+" ");
                            Department_map.put(s_blank[i-1].substring(0,s_blank[i-1].indexOf(matcher.group())).trim(),
                                    s_blank[i-1].substring(s_blank[i-1].indexOf(matcher.group()))+" "+Department_name);
                        }
                    }else if(Department_num_list.length==1&&Department_num_list[0].length()>0) {
                        for(int j=0;j<Department_num_list[0].trim().length();j++) {
                            Department_map.put(String.valueOf(Department_num_list[0].trim().charAt(j)), Department_name);
                        }
                    }else{
                        Department_map.put(Department_num.trim(), Department_name);
                    }
                }
            }
        }

        Map<String, String> author_department_map=new HashMap<String, String>();
        for(int i=0;i<author_name.size();i++) {
            String department="";
            if(author_name.size()==author_num.size()) {
                for(int j=0;j<author_num.get(i).length();j++) {
                    department+=Department_map.get(String.valueOf(author_num.get(i).charAt(j)))+",";
                }
                author_department_map.put(author_name.get(i), department);
            }
        }

        int keywords = 0;
        String article_keywords = "";
        try {
            if(s.toLowerCase().indexOf("keywords")!=-1) {
                keywords = s.toLowerCase().indexOf("keywords");
                article_keywords = s.substring(keywords+9,s.toLowerCase().indexOf("introduction"));
            }else if(s.toLowerCase().indexOf("index terms")!=-1) {
                keywords = s.toLowerCase().indexOf("index terms");
                article_keywords = s.substring(keywords+12,s.toLowerCase().indexOf("introduction"));
            }
            String article_abstract = s.substring(s.toLowerCase().indexOf("abstract")+9,keywords);
            System.out.println("-------------article_abstract--------------");
            System.out.println(article_abstract);
            jsonObject.put("article_abstract",article_abstract);
//
            System.out.println("-------------article_keywords--------------");
            System.out.println(article_keywords.split("\n")[0]);
            jsonObject.put("article_keywords",article_keywords.split("\n")[0]);
        }catch(Exception e) {
            System.out.println(e);
        }
        long now = System.currentTimeMillis();
//        System.out.println("convert OK! " + ((now - old) / 1000.0) + "秒");
        return jsonObject;
    }
}
