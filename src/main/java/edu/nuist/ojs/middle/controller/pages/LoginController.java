package edu.nuist.ojs.middle.controller.pages;


import com.alibaba.fastjson.JSONObject;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.interceptor.ContextInterceptor;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.stub.CallStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import javax.servlet.http.HttpServletRequest;

@RestController
public class
 LoginController {
    @Autowired
    private CallStub callStub;

    @Autowired
	private JournalSettingHelper jsh;

    @Autowired
    private ContextInterceptor contextInterceptor;

    @RequestMapping("/login/do")
    public JSONObject loginDo (
        HttpServletRequest request,
        @RequestParam String email,
        @RequestParam String password,
        @RequestParam String abbr,
        Model model
    ){
        JSONObject rst = new JSONObject();
        rst.put("flag", false);

        User u = null;
        Publisher p = null;
        if( abbr.equals("admin") ){
            u = callStub.login(email, password, -1); //是超级管理员登录
        }else{
            p = callStub.findPublisherByAbbr(abbr);
            if( p != null ){
                u = callStub.login(email, password, p.getId());     //出版社用户登录
            }
        }
        if( u != null && u.isDisabled() ){
            rst.put("banned", true);
            return rst;
        }

        if(  u != null && u.getUserId() != 0 ){
            if( u.isActived() ){
                rst.put("flag", true); 
                contextInterceptor.init(request.getSession());
                contextInterceptor.setUser(u, request.getSession());
                
                if( p!= null){
                    contextInterceptor.setPublisher(p, request.getSession());
                    String jsonstr = callStub.callStub("journalrole", String.class, "uid", u.getUserId());
                    
                    contextInterceptor.setRoles(jsh.getRoleForUser(jsonstr, u.getEmail()), request.getSession());
                }
            } else {
                rst.put("flag", true); 
                rst.put("info", "noactive");
            }
            
        }
        //清除从激活返回的显示提示信息的标志
        model.addAttribute("active", false);
        
        return rst;
    }
}
