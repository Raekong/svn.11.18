package edu.nuist.ojs.middle.controller;

import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.middle.interceptor.I18NAnnotation;
import edu.nuist.ojs.middle.interceptor.I18NInterceptor;
import edu.nuist.ojs.middle.resourcemapper.country.CountryComponent;
import edu.nuist.ojs.middle.resourcemapper.journal.JorunalFileType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;


@Controller
public class PageComponentRouter {
    @Value("${uploader.platform}")
    private String cloudStorePlatform; //文件存储的云平台，分为三种： OSS：阿里， COS：腾讯， LOCAL：本地

    public static final String COMPONENTI18NKEY = "component_key";


    //取回页面中控件，如上传，如autocomplete, 日期等，这些控制都是放在DIV中，最高的DIV中
    //添加属性 i18n 代表国际化配置，
    @RequestMapping("/component/{componentName}/{i18n}")
    @I18NAnnotation( configPoint = "component", configKeys = "")
    public String uploader(
        HttpServletRequest request,
        Model model,
        String i18n, //国际化要求
        @PathVariable String componentName
    ){
        model.addAttribute(I18NInterceptor.I18N, I18N.CN.equals(i18n));
        model.addAttribute("platform", cloudStorePlatform);
        //组件名和国际化的标识应该一致
        request.setAttribute(COMPONENTI18NKEY, componentName);   
        return "component/" + componentName ;
    }

    @RequestMapping("/home/{componentName}/{i18n}")
    @I18NAnnotation( configPoint = "component", configKeys = "")
    public String submitPaper(
        HttpServletRequest request,
        Model model,
        @PathVariable String i18n, //国际化要求
        @PathVariable String componentName
    ){
        model.addAttribute(I18NInterceptor.I18N, I18N.CN.equals(i18n));
        model.addAttribute("platform", cloudStorePlatform);
        //组件名和国际化的标识应该一致
        request.setAttribute(COMPONENTI18NKEY, componentName);   
        return "home/" + componentName ;
    }

    @Autowired
    private JorunalFileType jfileTypes;

    @Autowired
    private CountryComponent cc;

    @RequestMapping("/submit/{componentName}/{i18n}")
    @I18NAnnotation( configPoint = "component", configKeys = "")
    public String submitPage(
        HttpServletRequest request,
        Model model,
        @PathVariable String i18n, //国际化要求
        @PathVariable String componentName
    ){
        model.addAttribute(I18NInterceptor.I18N, I18N.CN.equals(i18n));
        model.addAttribute("platform", cloudStorePlatform);
        //组件名和国际化的标识应该一致
        request.setAttribute(COMPONENTI18NKEY, componentName);   

        if( componentName.equals("submit-upload-pop")){
            model.addAttribute("types", jfileTypes.getFiletypes());
        }

        if(componentName.equals("submit-author-pop")){
            model.addAttribute("countries", cc.getList().getCityList(i18n.equals(I18N.CN)));
        }
        return "submit/" + componentName ;
    }


}
