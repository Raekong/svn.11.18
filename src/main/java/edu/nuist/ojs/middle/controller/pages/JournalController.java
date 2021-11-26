package edu.nuist.ojs.middle.controller.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.nuist.ojs.common.entity.EmailConfigPoint;
import edu.nuist.ojs.common.entity.EmailTpl;
import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.Journal;
import edu.nuist.ojs.common.entity.Role;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.UserRoleRelation;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalRoleEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.context.Context;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.ContextInterceptor;
import edu.nuist.ojs.middle.journalsetting.params.JournalSection;
import edu.nuist.ojs.middle.resourcemapper.emailtpl.EmailRecvEnum;
import edu.nuist.ojs.middle.resourcemapper.emailtpl.EmailTplMapper;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalConfig;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalRoleMenuSetting;
import edu.nuist.ojs.middle.stub.CallStub;

@RestController
public class JournalController {

    @Autowired
    private CallStub callStub;

    @Autowired
    private ContextInterceptor contextInterceptor;

    @Autowired
    private JournalRoleMenuSetting menuSetting;

    @RequestMapping("/journal/create/do")
    @ContextAnnotation(configPoint = "journalcreate", configKeys = "u.publisherId,p.id")
    public JSONObject create(
        HttpSession session,
        @RequestParam String name,
        @RequestParam String abbr,
        @RequestParam String manageemail
    ){
        Long pid = ThymleafHelper.get(session, "publisherId", Long.class);
        if( pid == null) return null;

        JSONObject j = callStub.createJornal(abbr, name, manageemail, pid);
        if( j.getString("flag").equals("success") ){
            User u = j.getObject("user", User.class);
            Journal journal = j.getObject("journal", Journal.class);

            Role manager = callStub.callStub("findRoleByAbbr", Role.class, "abbr", JournalRoleEnum.MANAGER.getAbbr());
            UserRoleRelation ur = UserRoleRelation.builder()
                                .journalId(journal.getJournalId())
                                .userId(u.getUserId())
                                .roleId(manager.getId())
                                .build();

            callStub.callStub("saveUserRoleRelation", UserRoleRelation.class,  "userRoleRelation", ur);
        }
        return j;
    }

    @RequestMapping("/journal/querybyAbbrLike")
    @ContextAnnotation(configPoint = "querybyAbbrLike", configKeys = "p.id,u.email,u.root")
    public String querybyAbbrLike(HttpSession session, @RequestParam String abbreviation){
        Long pid = ThymleafHelper.get(session, "id", Long.class);
        return callStub.callStub("querybyAbbrLike", String.class, "pid", pid, "abbr", abbreviation);
    }

    @RequestMapping("/journal/list/query")
    @ContextAnnotation(configPoint = "journalcreate", configKeys = "p.id,u.email,u.root")
    public List list(HttpSession session){
        Long pid = ThymleafHelper.get(session, "id", Long.class);
        boolean root = ThymleafHelper.get(session, "root", Boolean.class);
        List journals = callStub.callStub("journallist", List.class, "pid", pid);
        if( root ) return journals;

        //返回类型转换，不转换会错
        ObjectMapper mapper = new ObjectMapper();
        List<Journal> list = mapper.convertValue(journals, new TypeReference<List<Journal>>() { });

        Context c = ThymleafHelper.getContext(session);
        List<Long> jids =  c.getManager();
        //根据用户角色来过滤其可以看到的期刊
        List<Journal> rst = new LinkedList<>();
        for(Journal j : list){
            for(Long jid : jids){
                if( jid == j.getJournalId()){
                    rst.add( (Journal)j );
                }
            }
        }
        return rst;
    }

    @RequestMapping("/journal/chanageOrder")
    @ContextAnnotation(configPoint = "journalchangeorder", configKeys = "u.root")
    public JSONObject changeOrder(
        HttpSession session,
        @RequestParam long jid,
        @RequestParam double order
    ){
        boolean root = ThymleafHelper.get(session, "root", Boolean.class);

        JSONObject rst = new JSONObject();
        if( !root ){
            rst.put("flag", false);
            return rst;
        }else{
            callStub.callStub("journalchangeorder", boolean.class, "jid", jid, "order", order);
            rst.put("flag", true);
            return rst;
        }
    }

    @RequestMapping("/journal/getAllSettingByJid")
    @ContextAnnotation(configPoint = "journalconfiggetall", configKeys = "s.journalId")
    public String  getAllSettingByJid(HttpSession sessoin){
        long jid = ThymleafHelper.get(sessoin, "journalId", long.class);
        return callStub.callStub("getallsetting", String.class, "journalId", jid);
    }
    

    @RequestMapping("/journal/save")  //保存contact和masthead， REVIEW SUBMIT两个页面配置
    @ContextAnnotation(configPoint = "journalconfigsave", configKeys = "s.journalId")
    public JournalSetting saveJournalSetting(
            HttpSession sessoin,
            @RequestParam String configPoint,
            @RequestParam String configContent){

        
        long jid = ThymleafHelper.get(sessoin, "journalId", long.class);
        
        JournalSetting journalSetting = JournalSetting.builder()
                .journalId(jid)
                .configPoint(configPoint)
                .configContent(configContent)
                .build();

        if(configPoint.equals(JournalConfigPointEnum.CONTACT)) {
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }
        else if(configPoint.equals(JournalConfigPointEnum.MASTHEAD)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }
        else if(configPoint.equals(JournalConfigPointEnum.SECTION)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Submit_Guidelines)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Submit_Requirements)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Require_Reviewers_Num)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Reviewer_Requirement)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.CoverLetter_Check)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Latex_Check)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Review_Score_Module)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Review_Due)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Response_Due)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Revision_Due)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Technical_Check)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Technical_Check_Editor)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Technical_Check_Authority)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Technical_Check_Editor_Name)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.CoverLetter_Requirement)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Simaliary_Check_First)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Simaliary_Check)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Total_Similar)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.First_Similar)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Second_Similar)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Third_Similar)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Review_Decline_Notify)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Review_Finish_Num_Notify)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Review_Result_Num)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Review_OverDue_Notify)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Review_OverDue_Notify_Time)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Review_OverDue_Notify_Period)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Similarity_Revision_Due)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Payment)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Basic_pages_Number)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Over_Charge)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Article_Processing_Charge)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Online_Transfer_Fee)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Wire_Transfer)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.About_Article_Processing_Charge)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }else if(configPoint.equals(JournalConfigPointEnum.Bank_Info)){
            return callStub.callStub("journalsetting", JournalSetting.class, "setting", journalSetting);
        }
        	
        return null;
    }

    @RequestMapping("/journal/getSetting")
    @ContextAnnotation(configPoint = "journalconfigsave", configKeys = "s.journalId,p.emailAddress,p.emailSender,p.host,p.password,p.port")
    public JournalSetting getSettingByJidAndConfigPoint(HttpSession session, @RequestParam String configPoint){
        long jid = ThymleafHelper.get(session, "journalId", long.class);                         
        JournalSetting js = callStub.callStub("getSetting",JournalSetting.class,"journalId",jid,"configPoint",configPoint);

        if(configPoint.equals(JournalConfigPointEnum.MASTHEAD)){
           if( js == null ){
               js =  JournalSetting.builder().configContent("").configPoint(configPoint).build();
           }

           String configStr = js.getConfigContent();
           if( configStr == null || "".equals(configStr)) js.setConfigContent("{}");

           JSONObject obj = JSONObject.parseObject( configStr );
           if( obj==null || obj.getString("emailsender") == null || "".equals(obj.getString("emailsender"))){
                if( obj == null) obj = new JSONObject();
                obj.put("email", ThymleafHelper.get(session, "emailAddress", String.class)); 
                obj.put("emailSender", ThymleafHelper.get(session, "emailSender", String.class)); 
                obj.put("host", ThymleafHelper.get(session, "host", String.class)); 
                obj.put("password", ThymleafHelper.get(session, "password", String.class)); 
                obj.put("port", ThymleafHelper.get(session, "port", int.class)); 
                js.setConfigContent(obj.toJSONString());
           }
        }
        return js;
    }

    //SubmitReview的配置
    @Autowired
    private JournalConfig journalconfig;
    @RequestMapping(value={"/journal/getSubmitReviewSetting", "/journal/getWorkFlowDefine"})
    @ContextAnnotation(configPoint = "getjournalconfig", configKeys = "s.journalId,p.emailAddress,p.emailSender,p.host,p.password,p.port")
    public JournalSetting getSubmitReviewSetting(HttpSession session, @RequestParam String configPoint){
        long jid = ThymleafHelper.get(session, "journalId", long.class);                         
        JournalSetting js = callStub.callStub("getSetting",JournalSetting.class,"journalId",jid,"configPoint",configPoint);

        if( js == null){
            return journalconfig.getConfig(configPoint);
        }
        return js;
    }

    @RequestMapping("/journal/getAllSectionsByJid")
    public String getSectionsByJournalId(HttpSession sessoin, long jid){
        return callStub.callStub("getsectionbyjid", String.class, "journalId", jid);
    }

    @RequestMapping("/journal/getAllSections")
    @ContextAnnotation(configPoint = "journalsection", configKeys = "s.journalId")
    public String getSectionsByJournalId(HttpSession sessoin){
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        return callStub.callStub("getsectionbyjid", String.class, "journalId", journalId);
    }

    @RequestMapping("/journal/getSectionByTitleLike")
    @ContextAnnotation(configPoint = "journalsection", configKeys = "s.journalId")
    public String getSectionsByTitleLike(HttpSession sessoin, @RequestParam String title){
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        return callStub.callStub("getSectionByTitleLike", String.class, "journalId", journalId, "title", title);
    }

    @RequestMapping("/journal/role/save")
    @ContextAnnotation(configPoint = "journalsection", configKeys = "s.journalId")
    public Role saveRole(  
                HttpSession sessoin,
                @RequestParam String abbr,
                @RequestParam ( required = false) String zh,
                @RequestParam ( required = false) String en,
                @RequestParam long orgRoleId){
         long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
         Role r = Role.builder().sameLevel(orgRoleId).abbr(abbr).zh(zh==null? "" : zh).en(en==null?"":en).journalId(journalId).build();
         return callStub.callStub("saveRole", Role.class, "role", r);
    }

    @RequestMapping("/journal/section/getById")
    @ContextAnnotation(configPoint = "journalsection", configKeys = "s.journalId")
    public JournalSection getSectionbyId(
        @RequestParam long id){
            return callStub.callStub("getsectionbyid", JournalSection.class, "id", id);
    }

    @RequestMapping("/journal/section/save")
    @ContextAnnotation(configPoint = "journalsection", configKeys = "s.journalId")
    public JournalSection saveSection(
                                    HttpSession sessoin,
                                    @RequestParam long id,
                                    @RequestParam String title,
                                    @RequestParam String email,
                                    @RequestParam boolean authority,
                                    @RequestParam String expireDate){
                                        
            long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
            JournalSection s = JournalSection
                            .builder()
                            .expireDay(expireDate)
                            .guid("")
                            .isAuthority(authority)
                            .open(true)
                            .sectionEditor(email)
                            .title(title)
                            .journalId(journalId)
                            .build();

            if( id != -1 ) s.setId(id);
        return callStub.callStub("savesection", JournalSection.class, "section", s);
    }

    @RequestMapping("/journal/section/open")
    public JournalSection openSection(@RequestParam long id, @RequestParam boolean open){
        return callStub.callStub("openSection",JournalSection.class,"id",id,"open",open);
    }

    @RequestMapping("/journal/section/order")
    public JournalSection orderSection(@RequestParam long id, @RequestParam double order){
        return callStub.callStub("orderSection",JournalSection.class,"id",id,"order",order);
    }

    @RequestMapping("/journal/getTeamUserByRole")
    @ContextAnnotation(configPoint = "getTeamuserbyrole", configKeys = "s.journalId,u.i18n,p.i18n")
    public List<JSONObject>  getTeamUserByRole(HttpSession sessoin, @RequestParam long rid){
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        List tmp = callStub.callStub("getTeamuserbyrole", List.class, "jid", journalId, "rid", rid);
        ObjectMapper mapper = new ObjectMapper();

        List<JSONObject> rst = new LinkedList<>();
        for( Object[] r : mapper.convertValue(tmp, new TypeReference<List<Object[]>>() { })){
            JSONObject o = new JSONObject();
            o.put("uid", r[0] );
            o.put("email", r[4] );
            o.put("name", r[20]==null? " " : r[20] );
            o.put("urrid", r[21] );
            rst.add(o);
        }
        return rst;
    }

    @RequestMapping("/journal/getAllEditor")
    @ContextAnnotation(configPoint = "removeMember", configKeys = "s.journalId,u.i18n,p.i18n")
    public String getAllEditor(HttpSession sessoin, @RequestParam String email){
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        return callStub.callStub("getAllEditor", String.class, "jid", journalId, "email", email);
    }

    @RequestMapping("/journal/team/remove")
    @ContextAnnotation(configPoint = "removeMember", configKeys = "s.journalId,u.i18n,p.i18n")
    public void removeMember(HttpSession sessoin, @RequestParam long urrid){
        callStub.callStub("removeTeamMember", String.class, "urrid", urrid);
    }

    @RequestMapping("/journal/getTeamStatis")
    @ContextAnnotation(configPoint = "getTeamStatis", configKeys = "s.journalId,u.i18n,p.i18n")
    public String getTeam(HttpSession sessoin){
        String i18n = ThymleafHelper.get(sessoin, "i18n", String.class);
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);

        List list =  callStub.callStub("queryteam", List.class, "jid", journalId);
        ObjectMapper mapper = new ObjectMapper();

        List<JSONObject> rst = new LinkedList<>();
        for( Object[] r : mapper.convertValue(list, new TypeReference<List<Object[]>>() { })){
            JSONObject o = new JSONObject();
            o.put("rid", r[0]); o.put("abbr", r[1]); o.put("jid", r[3]);
            o.put("samelevel", r[4]);  o.put("urrid",r[6] ); o.put("uid", r[7]); o.put("total", r[8]);
            
            if( i18n.equals(I18N.CN)){
                o.put("title", r[5]); 
            }else{
                o.put("title", r[2]);
            }
            rst.add(o);
        }
        return JSONObject.toJSONString(rst);
    }

    @RequestMapping("/jouranl/getOriginRole")
    @ContextAnnotation(configPoint = "getoriginrole", configKeys = "u.i18n,p.i18n")
    public String getOriginRole(HttpSession sessoin, @RequestParam String role){
        String i18n = ThymleafHelper.get(sessoin, "i18n", String.class);
        List roles = callStub.callStub("getoriginrole", List.class);

        ObjectMapper mapper = new ObjectMapper();
        List<Role> list = new LinkedList<>();
        for( Role r : mapper.convertValue(roles, new TypeReference<List<Role>>() { })){
            r.setI18N( i18n.equals(I18N.CN ));
            if( r.getName().indexOf( role) != -1){
                list.add(r);
            }
        }
        return JSONObject.toJSONString(list);
    }

    @RequestMapping("/jouranl/getAllRoleForJournal")
    @ContextAnnotation(configPoint = "getrole", configKeys = "u.i18n,p.id,p.i18n")
    public List getAllRoleForJournal(HttpSession sessoin){
        long pid = ThymleafHelper.get(sessoin, "id", long.class);
        String i18n = ThymleafHelper.get(sessoin, "i18n", String.class);

        List roles = callStub.callStub("findRoleForPublic", List.class , "pid", pid);
        ObjectMapper mapper = new ObjectMapper();
        List<Role> list = new LinkedList<>();
        for( Role r : mapper.convertValue(roles, new TypeReference<List<Role>>() { })){
            r.setI18N( i18n.equals(I18N.CN ));
            list.add(r);
        }
        return list;
    }

    @RequestMapping("/jouranl/findRoleByJournalId")
    @ContextAnnotation(configPoint = "getrole", configKeys = "s.journalId,u.i18n,p.i18n")
    public String getRoleForJournal(HttpSession sessoin, @RequestParam  String role){
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        String i18n = ThymleafHelper.get(sessoin, "i18n", String.class);

        List roles = callStub.callStub("findRoleByJournalId", List.class , "jid", journalId);
        ObjectMapper mapper = new ObjectMapper();
        List<Role> list = new LinkedList<>();
        for( Role r : mapper.convertValue(roles, new TypeReference<List<Role>>() { })){
            r.setI18N( i18n.equals(I18N.CN ));
            if( r.getName().toLowerCase().indexOf( role.toLowerCase() ) != -1){
                list.add(r);
            }
        }
        return JSONObject.toJSONString(list);
    }

    @RequestMapping("/jouranl/saveUserRoleRelation")
    @ContextAnnotation(configPoint = "rolerelationsave", configKeys = "s.journalId,u.i18n,p.i18n")
    public UserRoleRelation saveUserRoleRelation(
        HttpSession sessoin, 
        @RequestParam long uid, 
        @RequestParam long rid 
    ){
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        UserRoleRelation re = callStub.callStub(
            "findRoleRelationByJournalIdAndUserIdAndRoleId", 
            UserRoleRelation.class,
            "jid", journalId, "uid", uid, "rid", rid
        );

        if( re != null ){ //角色重复设置
            return null;
        }
        re = UserRoleRelation.builder().journalId(journalId).roleId(rid).userId(uid).build();
        return callStub.callStub("saveUserRoleRelation", UserRoleRelation.class, "userRoleRelation", re);
    }

    @RequestMapping("/journal/emailconfig/")
    @ContextAnnotation(configPoint = "emailconfig", configKeys = "s.journalId,u.i18n,p.i18n")
    public EmailConfigPoint getEmailConfigForJournal(
        HttpSession sessoin, 
        @RequestParam String configPoint
    ){
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        String obj = callStub.callStub("getEmailConfigForJournal", String.class, "jid", journalId, "configPoint", configPoint);
        return new EmailConfigPoint(JSONObject.parseObject(obj));
    }
    

    @Autowired
    private EmailTplMapper systemEmailConfig;

    @RequestMapping("/journal/emailconfig/all")
    public List<EmailConfigPoint> getAllEmailConfigForJournal(
        HttpSession sessoin
    ){
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        String obj = callStub.callStub("getAllEmailConfigForJournal", String.class, "jid", journalId);
        JSONArray arr = JSONObject.parseArray(obj);

        List<EmailConfigPoint> tmp = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            tmp.add( new EmailConfigPoint( arr.getJSONObject(i)) );
        }

        HashMap<String, EmailConfigPoint> rst = new HashMap<>();
        for( EmailConfigPoint ep : tmp){
           rst.put( ep.getConfigPoint(), ep );
        }

        ArrayList<EmailConfigPoint> eps = new ArrayList<>(); 
        for(String configPoint :  systemEmailConfig.getAllSystemConfigPoint()){
            //系统默认的邮件模板不允许更改
            if( configPoint.equals("Account Confirm") ||   configPoint.equals("Password Reset") )         
                continue;
            
            EmailTpl tpl = systemEmailConfig.getSystemTpl(configPoint);
            EmailConfigPoint ep = rst.get(configPoint);
            
            if( ep != null ){
                EmailTpl defaultTpl = ep.getDefault();
                if( defaultTpl == null ){
                    tpl.setDefaultTpl(true);
                }else{
                    tpl.setDefaultTpl(false);
                }
                ep.getTpls().add(tpl);
                eps.add( ep );
            }else{
                tpl.setDefaultTpl(true);
                eps.add( ep = systemEmailConfig.getEmailConfigPoint(configPoint) );
                
            }

        }
        return eps;
    }

    @RequestMapping("/journal/setting/email/default")
    public String emailTplDefault(
        HttpSession sessoin,
        @RequestParam long id
    ){  
        callStub.callStub("setDefaultEmailTpl", String.class, "tid", id);
        return "";
    }

    @RequestMapping("/journal/setting/email/setWithEmail")
    public String setConfigPointWithEmail(
        HttpSession sessoin,
        @RequestParam long id,
        @RequestParam String configPoint,
        @RequestParam Boolean flag
    ){  
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        callStub.callStub("setConfigPointWithEmail", String.class, "jid", journalId, "cid", id, "flag", flag, "configPoint", configPoint);
        return "";
    }

    @RequestMapping("/journal/setting/email/update")
    @ContextAnnotation(configPoint = "emailupdate", configKeys = "s.journalId,u.i18n,p.i18n")
    public String emailTplUpdate(
        HttpSession sessoin,
        @RequestParam String name,
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam long id,
        @RequestParam int recevier
    ){  
        JSONObject obj = callStub.callStub("getEmailTpl", JSONObject.class, "tid", id);
        EmailTpl tmpTpl = EmailTpl.get(obj.getJSONObject("jsonData"));
        tmpTpl.setId(obj.getLongValue("id"));
        tmpTpl.setDefaultTpl(obj.getBooleanValue("defaultTpl"));

        String i18n = ThymleafHelper.get(sessoin, "i18n", String.class);

        tmpTpl.setName(name); tmpTpl.setRecipient(recevier+""); 
        if( i18n.equals(I18N.CN)){
            tmpTpl.setTitleCH(title);
            tmpTpl.setTplZH(content);
        }else{
            tmpTpl.setTitleEN(title);
            tmpTpl.setTplEN(content);
        }

        return callStub.callStub("updateEmailTpl", String.class, "tid", id, "jsondata" , JSONObject.toJSONString(tmpTpl));
    
    }
       

    @RequestMapping("/journal/setting/email/getById")
    public EmailTpl emailTplGetById(
        HttpSession sessoin,
        @RequestParam long tid
    ){  
        JSONObject obj = callStub.callStub("getEmailTpl", JSONObject.class, "tid", tid);
        EmailTpl tmpTpl = EmailTpl.get(obj.getJSONObject("jsonData"));
        tmpTpl.setId(obj.getLongValue("id"));
        tmpTpl.setDefaultTpl(obj.getBooleanValue("defaultTpl"));
            
        return tmpTpl;
    }

    @RequestMapping("/journal/setting/email/getAllRecvDefine")
    public List<EmailRecvEnum> getRecvDefine(){
        return EmailRecvEnum.getAll();
    }


    @RequestMapping("/journal/setting/email/del")
    public String emailTplDel(
        HttpSession sessoin,
        @RequestParam long id
    ){  
        callStub.callStub("delEmailTpl", String.class, "tid", id);
        return "";
    }

    @RequestMapping("/journal/setting/email/copy")
    public String emailTplCopy(
        HttpSession sessoin,
        @RequestParam long id,
        @RequestParam String configPoint,
        @RequestParam boolean email
    ){  
        long journalId = ThymleafHelper.get(sessoin, "journalId", long.class);
        EmailTpl tmpTpl = null;
        if( id == 0){
            tmpTpl = systemEmailConfig.getSystemTpl(configPoint);
        }else{
            JSONObject obj = callStub.callStub("getEmailTpl", JSONObject.class, "tid", id);
            tmpTpl = EmailTpl.get(obj.getJSONObject("jsonData"));
            tmpTpl.setId(obj.getLongValue("id"));
            tmpTpl.setDefaultTpl(obj.getBooleanValue("defaultTpl"));
        }

        JSONObject obj = (JSONObject)JSONObject.toJSON(tmpTpl);
        obj.put("name", "Copy of " + tmpTpl.getName());
        callStub.callStub("saveEmailTpl", String.class,"jid", journalId, "configPoint", configPoint, "email", email, "defaultTpl", false, "jsonData", JSONObject.toJSONString(obj));
        return "";
    }
}
