package edu.nuist.ojs.middle.controller.article;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.Journal;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.article.Article;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.common.entity.review.ReviewAction;
import edu.nuist.ojs.common.entity.review.ReviewRecommendType;
import edu.nuist.ojs.common.entity.review.ReviewResult;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.I18NAnnotation;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;

@Controller
public class ReviewController {
    @Autowired
    private CallStub callStub;

    @Autowired 
    private MessageComponent mComponent;

    @Autowired 
    private WorkflowMailHelper mailHelper; 

    @Autowired 
    private ArticleFileHelper afHelper;  


    /**
     * 已经决定在审稿时，弹出另外一个窗口，与外审界面一样，不做区分
     */
    @RequestMapping("/review/{aid}")
    @I18NAnnotation(configPoint = "reviewpage",  configKeys = "reviewpage,home")
    public String reviewPage( HttpSession session, Model model, @PathVariable long aid){ 
        
        return "error";
    }

    
    @RequestMapping("/review/decision/{type}")
    @ContextAnnotation(configPoint = "decision", configKeys = "u.i18n,p.i18n")
    public String decisionPage(
        HttpSession session, 
        Model model,
        @PathVariable String type, 
        @RequestParam long aid, 
        @RequestParam long jid, 
        @RequestParam int rid 
    ){
        String i18n = ThymleafHelper.get(session, "i18n", String.class);
        model.addAttribute("type", type);
        String json = callStub.callStub("getReviewActionByAidAndRid", String.class,"aid", aid, "rid", rid);
        
        JSONArray arr = JSONArray.parseArray(json);
        List<JSONObject> actions = new LinkedList<>(); 
        for(int i=0; i<arr.size(); i++){
            ReviewAction ra = JSONObject.toJavaObject(arr.getJSONObject(i), ReviewAction.class);
            if(ra.getCurstate() == 3){
                JSONObject obj = new JSONObject();
                obj.put("ra", ra);
                ReviewResult rr = callStub.callStub("getReviewResult", ReviewResult.class,"raid", ra.getId());
                ReviewRecommendType rt = ReviewRecommendType.getByIndex(rr.getRecommendType());

                obj.put("resultType", i18n.equals(I18N.CN) ? rt.getZh(): rt.getEn());
                obj.put("result", rr); 
                if( rr.getFilesStr() != null ){
                    JSONArray f = JSONArray.parseArray(rr.getFilesStr());
                    for(int j=0; j<f.size(); j++){
                        JSONObject file = f.getJSONObject(j);
                        file.put( "url", mComponent.getFileUrl(file.getString("innerId")));
                    }
                    obj.put("files", f);
                }
                
                actions.add(obj);
            }
        }
        JournalSetting js = callStub.callStub("getSetting",JournalSetting.class,"journalId", jid,"configPoint", JournalConfigPointEnum.Payment);
        model.addAttribute("needPay", (js==null? "false" : js.getConfigContent()));
        model.addAttribute("roundFiles", afHelper.getFileForReviewRound(aid, rid));
        model.addAttribute("actions", actions);
        model.addAttribute("type", type);
        return "article/tab/review-decision";
    }

    @RequestMapping("/review/submit")
    @ResponseBody
    public String reviewSubmit( HttpSession session, long jid, long aid ,long raid, String author, String editor, String files, int recommend){ 

        callStub.callStub(
            "reveiwactionsubmit", String.class, 
            "raid", raid, 
            "authorRecom", author,  
            "editorRecom", editor, 
            "fileJsonStr", files,
            "recommendType", recommend
        );

        //monitor: 更新结束审稿 
        callStub.callStub(
            "monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "reviewaction",
                "type", "completed",
                "raid", raid
            )
        );

        //发送感谢信
        mailHelper.sendReviewMail(raid, "Review Thanks");

        //查看是否有拒稿提配置，并发信
        if(recommend == ReviewRecommendType.Decline.getIndex()){
            JournalSetting  remind = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid, "configPoint","Review Decline Notify");
            if( remind != null && "true".equals(remind.getConfigContent())){
                mailHelper.sendDeclineNotifyMail( aid, raid );
            }
        }

        //查看是否有审稿返回数量配置，并发信
        JournalSetting  notify = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid, "configPoint","Review Finish Num Notify");
        if( notify != null && "true".equals(notify.getConfigContent())){
            JournalSetting  notifyNum = callStub.callStub("getSetting", JournalSetting.class,"journalId", jid, "configPoint","Review Result Num");
            String cn = callStub.callStub("completeNumByRaid", String.class,"raid", raid);
            if( Integer.valueOf(cn) >= Integer.valueOf(notifyNum.getConfigContent())){
                mailHelper.sendResultNumNotifyMail( aid, raid,  Integer.valueOf(cn));
            }
        }
        return "";
    }

    @RequestMapping("/review/close/{raid}")
    @ResponseBody
    public String closeReview( @PathVariable long raid, @RequestParam long aid){
        callStub.callStub("reveiwactionclose", String.class,  "raid", raid);
        mailHelper.sendCloseRemind( aid, raid );
        return "";
    }

    @RequestMapping("/review/withdraw/{raid}")
    @ResponseBody
    public String withdrawReview( @PathVariable long raid){
        callStub.callStub("reveiwactionwithdraw", String.class,  "raid", raid);
        //monitor: 回撤重新统计
        callStub.callStub("monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "reviewactionwithdraw",
                "raid", raid
            )
        );
        return "";
    }

    @RequestMapping("/review/withdrawdecision/")
    @ResponseBody
    public String withdrawReview(  @RequestParam long aid,  @RequestParam long rid){
        callStub.callStub("withdrawdecision", String.class,  "aid", aid, "rid", rid);
        return "";
    }

    @RequestMapping("/review/getLinkMd5/{raid}")
    @ResponseBody
    public String getLinkMd5( @PathVariable long raid){
        return callStub.callStub("getReviweLinkByMD5", String.class,  "raid", raid);
    }

    @RequestMapping("/review/{type}/{raid}")
    @ResponseBody
    public String requestDo( @PathVariable long raid, @PathVariable String type){
        if( type.equals("accept")){
            int status = ReviewAction.REVIEW;
            callStub.callStub("reveiwactionupdate", String.class, "status", status, "raid", raid);
        }else if(type.equals("decline")){
            int status = ReviewAction.REJECT;
            callStub.callStub("reveiwactionupdate", String.class, "status", status, "raid", raid);
        }

        //monitor: 更新审稿接收与拒绝
        callStub.callStub("monitor", String.class, 
            "data",
            MonitorDataAssembly.assembly(
                "endpoint", "reviewaction",
                "type", type,
                "raid", raid
            )
        );
        return "";
    }

    @RequestMapping("/review/result/check")
    @ResponseBody
    public ReviewResult checkResult( @RequestParam long raid ){
        ReviewResult rr = callStub.callStub("getReviewResult", ReviewResult.class, "raid", raid);
        if( rr.getFilesStr() != null){
            JSONArray arr = JSONObject.parseArray( rr.getFilesStr());
            for(int i=0; i<arr.size(); i++){
                JSONObject t = arr.getJSONObject(i);
                String link = mComponent.getFileUrl( t.getString("innerId"));
                t.put("url", link);
            }
        }
        
        return rr;
    }


    /**
     * 通过MD5来外部访问REVIEW，这个页面是主要的
     * 这个再入的逻辑比较复杂，因为要接收给编辑查看审稿结果的地方，所以要做很多的改造
     * ，从LINK的链接进入，一般是后面不带OTHTER参数，这种进入的就是正常的页面
     * ，如果带有参数，则进入这个接口后，MODEL中会加入一个开关，控制页面显示与加载审稿结果
     */
    @RequestMapping("/review/extend/{actionid}")
    public String reviewPageForLink( HttpServletRequest request,  HttpSession session, Model model,  @PathVariable long actionid){ 
        ReviewAction ra = callStub.callStub("getReviewActionById", ReviewAction.class, "actionid", actionid);
        model.addAttribute("ra",  ra);
        
        if( ra.getCurstate() == ReviewAction.REQUESTED || ra.getCurstate() == ReviewAction.RESPONSEOVERDUE){
            model.addAttribute("requested",  true);
        }else {
            model.addAttribute("requested",  false);
        }

        if( ra.getCurstate() == ReviewAction.REVIEW || ra.getCurstate() == ReviewAction.REVIEWOVERDUE){
            model.addAttribute("reviewing",  true); 
        }else {
            model.addAttribute("reviewing",  false);
        }
        //close是置ACTION的标记，不是切换状态，这个判断不一样
        if( ra.getCurstate() == ReviewAction.COMPLETE || ra.getCurstate() == ReviewAction.CLOSE || ra.isClosed()){
            model.addAttribute("requested",  false);
            model.addAttribute("reviewing",  false);//一旦结束，就全关闭了
            model.addAttribute("over",  true);
        }else {
            model.addAttribute("over",  false);
        }

        //这个参数表示是从编辑那边查看跳至新窗口打开的，一般情况下这个参数不存在
        if( request.getAttribute("view") != null){
            model.addAttribute("viewResult", "true");
        }else{
            model.addAttribute("viewResult", "false");
        }

        JSONArray arr = JSONArray.parseArray(ra.getFileJson());
        for(int i=0; i<arr.size(); i++){
            String link = arr.getJSONObject(i).getString("innerId");
            arr.getJSONObject(i).put("link", mComponent.getFileUrl(link));
        }
        model.addAttribute("files",  arr);

        String a = callStub.callStub("getArticleById", String.class, "aid", ra.getArticleId());
        Article article = Article.getArticle(JSONObject.parseObject(a));
        model.addAttribute("extends", true);
        model.addAttribute("a", article);

        Journal j = callStub.callStub("journalbyid", Journal.class, "jid", article.getJid());
        model.addAttribute("j", j);

        Publisher p = callStub.findPublisherById(article.getPid()); 
        model.addAttribute("i18n",  p.getI18n().equals(I18N.CN));   //出版社的语种
        model.addAttribute("p", p);
        return "article/review";
    }

    

    @RequestMapping("/article/history/{mid}")
    @ContextAnnotation(configPoint = "history", configKeys = "u.userId,u.i18n,u.root,p.i18n,p.id")
    public String historyPage(
        HttpSession session, 
        Model model, 
        @PathVariable long mid
    ){
        Message m = callStub.callStub("getMessage", Message.class, "id", mid); 
        
        model.addAttribute("msg", m);
        if(m.getAppendsJSONStr()!=null){
            model.addAttribute("files", JSONArray.parseArray(m.getAppendsJSONStr()));
        }
       
        return "article/tab/historymessage";
    }

}
