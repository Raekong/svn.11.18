package edu.nuist.ojs.middle.controller.pages;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;

import edu.nuist.ojs.common.entity.EmailFile;
import edu.nuist.ojs.common.entity.EmailTpl;
import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.Link;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.ContextInterceptor;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.stub.CallStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

import edu.nuist.ojs.middle.context.Context;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.datamonitor.DataMonitorAnnotation;
import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.email.MessageTplComponent;

@RestController
public class UserController {
    @Autowired
    private CallStub callStub;

    @Autowired
    private MessageComponent mComponent;

    @Autowired
    private ContextInterceptor contextInterceptor;

    @Autowired
    private JournalSettingHelper jsh;

    @RequestMapping("/user/i18n")
    @ContextAnnotation(configPoint = "setI18n", configKeys = "u.userId") //context中只有USER,和角色列表，其它不填
    public JSONObject setI18N(HttpServletRequest request, @RequestParam String lang) {
        long uid = (Long) request.getSession().getAttribute("userId");
        User u = callStub.setUsreI18N(uid, lang);
        ((Context) request.getSession().getAttribute(ContextInterceptor.CONTEXT)).setUser(u);
        ;

        Context temp = (Context) request.getSession().getAttribute("tempContext");
        request.getSession().setAttribute("Context", temp);
        System.out.println("finish");
        return new JSONObject();
    }

    @RequestMapping("/user/logout")
    public JSONObject logout(HttpSession session) {
        session.invalidate();
        return new JSONObject();
    }

    @RequestMapping("/user/getByEmailAndPid")
    @ResponseBody
    @ContextAnnotation(configPoint = "uesrRegist", configKeys = "p.i18n,p.id")
    public String getByEmailAndPid(HttpSession session, String email) {
        long pid = (long) session.getAttribute("id");
        String users = callStub.callStub("getUserByEmailAndPid", String.class, "email", email, "pid", pid);
        return users;
    }

    @RequestMapping("/user/getSectionEditorByEmailAndJid/{jid}")
    @ResponseBody
    @ContextAnnotation(configPoint = "uesrRegist", configKeys = "p.i18n,p.id")
    public String getSectionEditorByEmailAndPid(
            HttpSession session,
            @PathVariable long jid,
            String email
    ) {
        String users = callStub.callStub("getSectionEditorByEmailAndJid", String.class, "email", email, "jid", jid);
        return users;
    }

    @RequestMapping("/user/getByNameAndPid")
    @ResponseBody
    @ContextAnnotation(configPoint = "uesrRegist", configKeys = "p.i18n,p.id")
    public String getByNameAndPid(HttpSession session, String name) {
        long pid = (long) session.getAttribute("id");
        String users = callStub.callStub("getByNameAndPid", String.class, "name", name, "pid", pid);
        return users;
    }

    @RequestMapping("/user/queryUserWithRidAndNameAndEmail")
    @ContextAnnotation(configPoint = "uesrRegist", configKeys = "p.i18n, p.id")
    public String queryUserWithRidAndNameAndEmail(
            HttpSession session,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam long rid,
            @RequestParam int pageNumber,
            @RequestParam int pageSize) {
        //rid==-1 表示不需要匹配
        long pid = ThymleafHelper.get(session, "id", long.class);
        return callStub.callStub("userQuery", String.class, "page", pageNumber, "size", pageSize, "name", name, "email", email, "pid", pid, "rid", rid);
    }

    @RequestMapping("/user/create")
    @ContextAnnotation(configPoint = "uesrRegist", configKeys = "p.i18n, p.id")
    public User createUser(
            HttpSession session,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String first,
            @RequestParam String middle,
            @RequestParam String last
    ) {
        String i18n = ThymleafHelper.get(session, "i18n", String.class);
        long pid = ThymleafHelper.get(session, "id", long.class);
        User u = User.builder()
                .affiliation("")
                .country("")
                .disabled(false)
                .isActived(true)
                .email(email)
                .firstname(first)
                .middlename(middle)
                .i18n(i18n)
                .interests("")
                .publisherId(pid)
                .password(password)
                .lastname(last)
                .build();

        return callStub.regist(u);
    }


    @Autowired
    private MessageTplComponent tplComponent;

    @Autowired
    private MessageComponent messageComponent;

    @Value("${global.linkserver}")
    private String linkserver;

    @RequestMapping("/user/regist")
    @ResponseBody
    @ContextAnnotation(configPoint = "uesrRegist", configKeys = "p.i18n, p.id")
    public User regist(
            HttpSession session,
            @RequestParam HashMap<String, String> user
    ) {
        long pid = ThymleafHelper.get(session, "id", long.class);
        String i18n = ThymleafHelper.get(session, "i18n", String.class);


        User u = User.builder()
                .affiliation(user.get("user[affiliation]"))
                .country(user.get("user[country]"))
                .disabled(false)
                .email(user.get("user[email]"))
                .firstname(user.get("user[firstname]"))
                .middlename(user.get("user[midname]"))
                .i18n(i18n)
                .interests(user.get("user[interest]"))
                .publisherId(pid)
                .isActived(false)
                .password(user.get("user[password]"))
                .lastname(user.get("user[lastname]"))
                .build();

        User user1 = callStub.regist(u);
        if (user1.getUserId() != -1)
            sendAccountConfirmEmail(pid, user1, u.getPassword(), i18n);

        return user1;
    }

    //SEND ACCOUNT CONFIRM EMAIL
    private void sendAccountConfirmEmail(long pid, User u, String password, String i18n) {
        Publisher p = callStub.findPublisherById(pid);

        JSONObject obj = new JSONObject();
        obj.put("pid", pid);
        obj.put("id", u.getUserId());
        Link l = Link.builder().api("/user/active").jsonData(obj.toJSONString()).build();
        String md5 = l.getMD5();

        callStub.callStub("savelink", Link.class, "link", l);

        HashMap<String, String> params = new HashMap<>();
        params.put("#username#", u.getUsername());
        params.put("#email#", u.getEmail());
        params.put("#password#", password);
        params.put("#publisheraddress#", p.getContact());
        params.put("#publishername#", p.getName());
        params.put("#link#", linkserver + md5);

        JSONObject isEmail = new JSONObject();
        EmailTpl tpl = tplComponent.getDefaultTpl(0, "Account Confirm", isEmail);
        String title = tpl.renderTitle(params, i18n.equals(I18N.CN));
        String content = tpl.render(params, i18n.equals(I18N.CN));

        messageComponent.SendPublishMessage(true, pid, u, "Account Confirm", title, content, null);
    }

    private void sendResetPasswordEmail(long pid, String username, String email, String password, String contact, String publishername, String i18n) {

        HashMap<String, String> params = new HashMap<>();
        params.put("#username#", username);
        params.put("#password#", password);
        params.put("#publisheraddress#", contact);
        params.put("#publishername#", publishername);

        JSONObject isEmail = new JSONObject();
        EmailTpl tpl = tplComponent.getDefaultTpl(0, "Password Reset", isEmail);
        String title = tpl.renderTitle(params, i18n.equals(I18N.CN));
        String content = tpl.render(params, i18n.equals(I18N.CN));

        messageComponent.SendPublishMessage(
                true,
                pid,
                User.builder().username(username).email(email).build(),
                "Account Confirm",
                title,
                content,
                null);
    }


    @RequestMapping("/user/resetpassword/do")
    public Map<String, Object> resetPassword(@RequestParam String pulisherAbbr, @RequestParam String email) {
        Map<String, Object> rst = callStub.resetPassword(pulisherAbbr, email);
        Publisher p = callStub.findPublisherByAbbr(pulisherAbbr);

        if (rst.get("flag").toString().equals("true")) {
            sendResetPasswordEmail(
                    p.getId(),
                    rst.get("username").toString(),
                    email,
                    rst.get("code").toString(),
                    p.getContact(),
                    p.getName(),
                    p.getI18n());
        }
        return rst;
    }

    @RequestMapping("/user/resendActive")
    public JSONObject resendActive(HttpSession session) {
        String email = (String) session.getAttribute("email");
        String abbr = (String) session.getAttribute("publisher");

        Publisher p = callStub.findPublisherByAbbr(abbr);
        User u = callStub.findByPublisherIdAndEmail(p.getId(), email);
        Message m = callStub.getMessage(u.getUserId(), "Account Confirm");
        JSONObject rst = new JSONObject();
        if (m != null) { //如果找到当时注册的消息就直接重发
            m.setEmailId("");
            messageComponent.resend(m);
            rst.put("flag", true);
        } else {
            rst.put("flag", false);
        }
        return rst;

    }

    @RequestMapping("/user/findById")
    @ResponseBody
    public User findById(@RequestParam long id) {
        return callStub.callStub("findUserById", User.class, "id", id);
    }

    @RequestMapping("/user/updateInformation")
    @ContextAnnotation(configPoint = "updateProfile", configKeys = "u.userId,u.i18n,p.i18n,p.id")
    @DataMonitorAnnotation(configPoint = "userinfomodify")
    @ResponseBody
    public String updateProfile(HttpSession session,
                                @RequestParam HashMap<String, String> user
    ) {
        long pid = ThymleafHelper.get(session, "id", long.class);
        long uid = ThymleafHelper.get(session, "userId", long.class);
        String i18n = ThymleafHelper.get(session, "i18n", String.class);

        User u = User.builder()
                .affiliation(user.get("user[affiliation]"))
                .country(user.get("user[country]"))
                .disabled(false)
                .email(user.get("user[email]"))
                .firstname(user.get("user[firstname]"))
                .middlename(user.get("user[middlename]"))
                .i18n(i18n)
                .interests(user.get("user[interests]"))
                .publisherId(pid)
                .isActived(true)
                .password(user.get("user[password]"))
                .lastname(user.get("user[lastname]"))
                .userId(uid)
                .build();
        //System.out.println( u );
        MonitorDataAssembly.assembly(session, "userId", uid);
        return callStub.callStub("updateprofile", String.class, "u", u);
    }


    @RequestMapping("/user/active")
    public User activeUser(@RequestParam long id) {
        return callStub.callStub("activeUser", User.class, "id", id);
    }

    @RequestMapping("/user/disable")
    public User disableUser(@RequestParam long id) {
        return callStub.callStub("disableUser", User.class, "id", id);
    }

    @RequestMapping("/user/setPassword666")
    public User userResetPassword(@RequestParam long id) {
        return callStub.callStub("userResetPassword", User.class, "id", id);
    }

    @RequestMapping("/user/sendmessage")
    @ContextAnnotation(configPoint = "sendmessage", configKeys = "u.userId,u.i18n,p.i18n,p.id")
    public Message sendMesage(
            HttpSession session,
            @RequestParam String title,
            @RequestParam boolean isEmail,
            @RequestParam String content,
            @RequestParam String recevier,
            @RequestParam String attachments
    ) {

        long pid = ThymleafHelper.get(session, "id", long.class);
        JSONObject utmp = JSONObject.parseObject(recevier);
        User recevierUser = User.builder()
                .userId(utmp.getLongValue("id"))
                .email(utmp.getString("email"))
                .username(utmp.getString("name"))
                .build();

        List<EmailFile> attachmentFiles = new LinkedList<>();

        JSONArray files = JSONObject.parseArray(attachments);
        for (int i = 0; i < files.size(); i++) {
            EmailFile f = mComponent.getFile(files.getJSONObject(i));
            attachmentFiles.add(f);
        }
        Message m = mComponent.SendPublishMessage(
                isEmail,
                pid,
                recevierUser,
                "User Manager",
                title,
                content,
                attachmentFiles
        );

        return m;
    }


//    @RequestMapping("/user/turn")
//    @ContextAnnotation(configPoint = "userturn", configKeys = "u.userId,p.id")
//    public User turnUser(HttpSession session,
//                         @RequestParam long id
//
//    ) {
//        //保存原用户的context为temp，存在session中
//        Context temp = ThymleafHelper.getContext(session);
//        session.setAttribute("tempContext",temp);
//        System.out.println("id = " + id);
//        //新用户登录
//        long pid = ThymleafHelper.get(session, "id", long.class);
//        User u = callStub.getUserById(id);
//        System.out.println("u = " + u);
//        u = callStub.loginAs(u.getEmail(), u.getPassword(), pid);
//
//        //更新新用户的Context
//        String abbr = (String) session.getAttribute("publisher");
//        Publisher p = callStub.findPublisherByAbbr(abbr);
//
//        //查看session中所有值
//        System.out.println("替换context前");
//        Enumeration<String> attrs = session.getAttributeNames();
//        // 遍历attrs中的
//        while(attrs.hasMoreElements()){
//            // 获取session键值
//            String name1 = attrs.nextElement().toString();
//            // 根据键值取session中的值
//            Object vakue = session.getAttribute(name1);
//            // 打印结果
//            System.out.println("------" + name1 + ":" + vakue +"--------\n");
//        }
//
//
//        if (u != null && u.getUserId() != 0) {
//            if (u.isActived()) {
//                contextInterceptor.init(session);
//                contextInterceptor.setUser(u, session);
//
//                if (p != null) {
//                    contextInterceptor.setPublisher(p, session);
//                    String jsonstr = callStub.callStub("journalrole", String.class, "uid", u.getUserId());
//                    contextInterceptor.setRoles(jsh.getRoleForUser(jsonstr, u.getEmail()), session);
//                }
//            }
//            System.out.println("替换context后");
//            Enumeration<String> attrs1 = session.getAttributeNames();
//            // 遍历attrs中的
//            while(attrs1.hasMoreElements()){
//                String name1 = attrs1.nextElement().toString();
//                Object vakue = session.getAttribute(name1);
//                System.out.println("------" + name1 + ":" + vakue +"--------\n");
//            }
//        }
//        return u;
//    }
//
//    @RequestMapping("/user/turnBack")
//    public JSONObject turnBackUser(HttpSession session){
//        Context temp = (Context) session.getAttribute("tempContext");
//        session.setAttribute("Context", temp);
//        return new JSONObject();
//
//    }
}
