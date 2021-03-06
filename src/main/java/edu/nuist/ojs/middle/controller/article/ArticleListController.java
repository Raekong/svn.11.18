package edu.nuist.ojs.middle.controller.article;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.article.Article;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.datamonitor.datamonitormap.MonitorMapper;
import edu.nuist.ojs.middle.file.COSFileDownloader;
import edu.nuist.ojs.middle.file.OOSFileDownloader;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.interceptor.I18NAnnotation;
import edu.nuist.ojs.middle.interceptor.I18NInterceptor;
import edu.nuist.ojs.middle.resourcemapper.article.ArticleStatusMapper;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;
import edu.nuist.ojs.middle.workflow.WorkFlowMainStateMachine;
import edu.nuist.ojs.middle.workflow.copyedit.CopyeditTabHelper;
import edu.nuist.ojs.middle.workflow.payment.PayTabHelper;
import edu.nuist.ojs.middle.workflow.review.ReviewTabHelper;
import edu.nuist.ojs.middle.workflow.similarcheck.SimilarCheckTabHelper;
import edu.nuist.ojs.middle.workflow.submit.SubmitTabHelper;

@Controller
public class ArticleListController {
    public static final String AUTHOR = "author";
    public static final String CONTRIBUTOR = "contributor";
    public static final String REVIEWER = "reviewer";

    @Autowired
    private CallStub callStub;

    @Autowired
    private WorkFlowMainStateMachine stateMachine; 

    @Value("${uploader.platform}")
    private String platform;
    
    @Autowired
    private OOSFileDownloader oDownloader;

    @Autowired
    private COSFileDownloader cDownloader;

    @Autowired
    private ArticleStatusMapper statusMapper;

    @Autowired
    private ArticleFileHelper aFileHelper;
    
    @Autowired
    private ArticleUserRoleHelper userRoleHelper;

    public static final String ARTICLE_ROLE_PRIFIX = "article-role-";
    public static final String ARTICLE_BOARD_PRIFIX = "article-board-";
    
    
    /**
     * ??????????????????
     * ?????????
     *  1. ????????????????????????????????????????????????????????????
     *  r.manager ? ????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *  r.prereview          //????????????????????????????????????????????????
     *  r.review.authorityeditor    //???????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *  r.review.responsingeditor   //????????????????????????SECTION????????????????????????, ???????????????????????????????????????????????????
     *  r.review.editor             //????????????????????????????????????
     *  r.author                    //????????????
     *  r.prereview    //???????????????????????????????????????????????????????????????REVIEW??????SIMILAR CHECK??????????????????
     *  r.similarcheck      //??????????????????
     *  r.playment         //??????????????????
     *  2. ???????????????????????????????????????SESSION?????????????????????????????????TAB????????????????????????????????????????????????
     *  3. ?????????????????????????????????????????????
     *  4. ??????????????????????????????????????????HISTORY??????WORKFLOW?????????????????????ROLE?????????????????????????????????????????????
     *  5. ???????????????
     *  6. ????????????JS???????????????????????????????????????????????????????????????
     * @return
     */
    @RequestMapping("/article/{id}")
    @ContextAnnotation(configPoint = "article", configKeys = "u.userId,u.i18n,u.root,p.i18n,u.email,u.publisherId,p.name,p.abbr")
    @I18NAnnotation(configPoint = "article", configKeys = "home")
    public String article(HttpSession session,  Model model,  @PathVariable long id){
        try {
            long uid = ThymleafHelper.get(session, "userId", long.class); 
            String a = callStub.callStub("getArticleById", String.class, "aid", id);
            Article article = Article.getArticle(JSONObject.parseObject(a));


            HashMap<String, Boolean> roles = userRoleHelper.getRigthByUidAndAid(id, uid);
            session.setAttribute(ARTICLE_ROLE_PRIFIX+id, roles); //????????????????????????
            session.setAttribute(ARTICLE_BOARD_PRIFIX+id, userRoleHelper.getBoard(id));//????????????
            
            ThymleafHelper.home(model, session);
            model.addAttribute("aid", id);
            model.addAttribute("jid", article.getJid());
            model.addAttribute("pid", article.getPid());
            model.addAttribute("role", roles);//?????????????????????????????????????????????
            model.addAttribute("isRoot", ThymleafHelper.get(session, "root", boolean.class));
            model.addAllAttributes(stateMachine.getPageHeadInfo(id, roles, I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N))));
            model.addAttribute(
                    I18NInterceptor.I18N,
                    I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N)));

            return "article/page";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Autowired
    private SubmitTabHelper sHelper;
    @Autowired
    private ReviewTabHelper rhelper;
    @Autowired
    private HistoryHelper hHhelper;
    @Autowired
    private SimilarCheckTabHelper scHelper; 
    @Autowired
    private PayTabHelper pHelper; 
    @Autowired
    private CopyeditTabHelper cHelper;

    @RequestMapping("/article/tab/{aid}/{tab}")
    @ContextAnnotation(configPoint = "tabs", configKeys = "u.i18n,u.root,p.i18n,u.email,u.publisherId,p.name,p.abbr")
    public String getTab(
        HttpSession session, 
        Model model,  
        @PathVariable long aid, 
        @RequestParam long jid, 
        @PathVariable String tab
    ){
        HashMap<String, Object> params = new HashMap<String, Object>();
        String i18n = ThymleafHelper.get(session, "i18n", String.class); 
        switch(tab){
            case "submit":
                params = sHelper.render(aid); 
                break;
            case "review":
                params.put("rounds", rhelper.getRounds(aid, i18n.equals(I18N.CN))); 
                model.addAttribute("board", session.getAttribute(ARTICLE_BOARD_PRIFIX+ aid));
                model.addAttribute("roles", session.getAttribute(ARTICLE_ROLE_PRIFIX + aid));
                break;
            case "similarcheck":
                model.addAttribute("board", session.getAttribute(ARTICLE_BOARD_PRIFIX+ aid));
                model.addAttribute("roles", session.getAttribute(ARTICLE_ROLE_PRIFIX + aid));
                model.addAttribute("rounds", scHelper.getRounds(aid, i18n.equals(I18N.CN)));
                break;
            case "payment":
                model.addAttribute("bankinfo", pHelper.getBankInfo(jid));
                model.addAttribute("files",  aFileHelper.getPaymentFiles(aid));
                model.addAttribute("pays", pHelper.getPaymentsByAid(aid));
                model.addAttribute("board", session.getAttribute(ARTICLE_BOARD_PRIFIX+ aid));
                model.addAttribute("roles", session.getAttribute(ARTICLE_ROLE_PRIFIX + aid));
                break;
            case "copyedit":
                params.put("rounds", cHelper.getRounds(aid, i18n.equals(I18N.CN))); 
                model.addAttribute("board", session.getAttribute(ARTICLE_BOARD_PRIFIX+ aid));
                model.addAttribute("roles", session.getAttribute(ARTICLE_ROLE_PRIFIX + aid));
                break;
            case "history":
                model.addAttribute("roles", session.getAttribute(ARTICLE_ROLE_PRIFIX + aid));
                params.put("histories", JSONArray.parseArray(hHhelper.getHistoryByAid(aid))); 
                break;
        }
        //????????????????????????Model???????????????????????????????????????
        model.addAttribute("useremail", ThymleafHelper.get(session, "email", String.class));
        model.addAllAttributes(params);
        return "article/tab/"+tab;
    }

    @RequestMapping("/article/review/review-change-pop")
    public String changePop(HttpSession session, Model model){
        return "article/tab/review-change-pop";
    }

    @RequestMapping("/article/review/review-invite-pop")
    public String assignEditor(HttpSession session, Model model, long jid, long aid, long rid){
        model.addAttribute("rid", rid);
        model.addAttribute("suggests", JSONArray.parseArray( callStub.callStub("getSuggestReviewer", String.class, "aid", aid)));
        model.addAttribute("lasts", JSONArray.parseArray( callStub.callStub("getLastRoundReviewer", String.class, "aid", aid, "rid", rid)));
        String[] dus = rhelper.getReviewDue(jid);
        model.addAttribute("responseDue", dus[0]);
        model.addAttribute("reviewDue", dus[1]);

        model.addAttribute("files", aFileHelper.getFileForReviewRound(aid, (int) rid));
        return "article/tab/review-invite-pop";
    }

    @RequestMapping("/article/review/assign-editor-pop")
    public String assignEditor(HttpSession session, Model model, long aid){
        model.addAttribute("roles", session.getAttribute(ARTICLE_ROLE_PRIFIX + aid));
        return "article/tab/assign-editor-pop";
    }

    @Autowired
    private MonitorMapper maper;

    @RequestMapping("/paperlist")
    @ContextAnnotation(configPoint = "journalconfig", configKeys = "u.userId,u.i18n,u.root,p.i18n,u.email,u.publisherId,p.id,p.name,p.abbr")
    @I18NAnnotation(configPoint = "paperlist", configKeys = "paperlist,home")
    public String paperList(HttpSession session, Model model){
        try {
            ThymleafHelper.home(model, session);
            model.addAttribute("isRoot", ThymleafHelper.get(session, "root", boolean.class));
            model.addAttribute(
                    I18NInterceptor.I18N,
                    I18N.CN.equals(session.getAttribute(I18NInterceptor.I18N)));
            
            long uid = ThymleafHelper.get(session, "userId", long.class);
            long pid = ThymleafHelper.get(session, "id", long.class);
            //??????????????????????????????????????????
            /**
             *  rst.put("manager", false);
                rst.put("editor", false);
                rst.put("similarcheck", false);
                rst.put("financial", false);
                rst.put("copyeditor", false);
                rst.put("perreviewer", false);
             */
            JSONObject showList = userRoleHelper.getEditorListBoard(pid, uid);
            model.addAttribute("listroles", showList);
            JSONObject obj = callStub.callStub("getlistboardsetting", JSONObject.class, "uid", uid);
            if( obj != null )
                model.addAttribute("customshowconfig", obj);
            
            return "article/list";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @RequestMapping("/article/list")
    @ResponseBody
    @ContextAnnotation(configPoint = "articlelist", configKeys = "u.userId,u.i18n,u.root,p.i18n")
    public String getListByType(
        @RequestParam String type,            
        @RequestParam  int  pageNumber,
        @RequestParam  int pageSize,
        HttpSession session){
        long uid = ThymleafHelper.get(session, "userId", long.class); 
        String json = callStub.callStub("getArticleByType", String.class, "type", type, "uid", uid, "page", pageNumber, "size", pageSize);
        JSONArray obj = JSONObject.parseObject(json).getJSONArray("content"); 
        String i18n = ThymleafHelper.get(session, "i18n", String.class);

        for(int i=0; i<obj.size(); i++){
            JSONObject o = obj.getJSONObject(i);
            JSONObject s = o.getJSONObject("status");
            if( s != null ){
                String status = s.getString("status");
                //System.out.println( "==============---------------------------------" );
                //System.out.println( status );
                o.put("status", statusMapper.get(status).getStatus(i18n.equals(I18N.CN)));
            }
        }

        return JSON.toJSONString(obj);
    }

    @RequestMapping("/article/file/del/{fid}")
    @ResponseBody
    public String delFile(HttpSession session, Model model, @PathVariable long fid){
        callStub.callStub("delArticleFile", String.class, "fid", fid);
        return "";
    }

    @RequestMapping("/article/file/upload")
    @ResponseBody
    public String reuploadReviewFile(
        HttpSession session, 
        Model model, 
        @RequestParam String filetype,
        @RequestParam String innerId,
        @RequestParam String originName,
        @RequestParam long aid,
        @RequestParam long rid
    ){
        if( filetype.toLowerCase().indexOf("files") == -1){
            boolean canReupload = callStub.callStub("canReupload", boolean.class, "aid", aid, "version", "REVIEW-"+rid, "filetype", filetype);
            if( !canReupload ) return "false";
        }
        
        ArticleFile file = ArticleFile
                        .builder()
                        .aid(aid)
                        .fileType(filetype)
                        .innerId(innerId)
                        .originName(originName)
                        .version("REVIEW-"+rid)
                        .build();

        callStub.callStub("saveArticleFile", ArticleFile.class, "file", file);
        return "true";
    }

    @RequestMapping("/article/info/{type}")
    @ResponseBody
    @ContextAnnotation(configPoint = "articleInfo", configKeys = "u.userId")
    public JSONObject getArticleInfo(
        @PathVariable String type,
        @RequestParam long aid,
        HttpSession session){

        JSONObject rst = null;
        switch(type){
           case "basic":
                String a = callStub.callStub("getArticleById", String.class, "aid", aid);
                rst = JSONObject.parseObject(a);
                break;
       }
       return rst;
    }

    @RequestMapping(value = "/article/file/download")
    public void download(@RequestParam String appends, HttpServletRequest request, HttpServletResponse response){
        if(platform.equals("COS")){
            cDownloader.exec(appends, request, response);
        }else if(platform.equals("OSS")){
            oDownloader.exec(appends, request, response);
        }
        return;
    }

}
