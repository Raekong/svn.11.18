package edu.nuist.ojs.middle.workflow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.StyledEditorKit.BoldAction;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.stub.CallStub;
/**
 *  这个控件用来生成一个用户对于某一篇文章的所有访问控制
 *  1. 他能访问几个卡片页？
 *  2. 他访问的卡片页中哪些控件？
 *      1. 期刊主管，可以访问一切，拥有一切权利
 *      2. 普通编辑，可以访问自己的卡片页，SUBMISSION, HISTORY，
 *      3. 用户可以访问自己的卡片页的部分内容，但显然不能控制控件
 * 
 *  3. 所以我们分一下，控件和面板是编辑才能操作
 *  
 *  判断的流程，1，是不是主管？如果是一切皆可
 *             2，是什么编辑？如果是，只能访问自己的卡片页，由ROLE的ID和对应的卡片页关系来确定
 *             3，如果是作者，卡片页都能访问，但是在卡片页中受限
 * 
 *  所以卡片页的判断就是，判断用户对于页面的身份：是主管，还是编辑，还是作者或参与者
 *  如果是编辑，是什么编辑，应该对应什么卡片页？ 如此，解决卡片页的问题
 * 
 *  那么还有一个问题，怎么去实现用户对卡片页内容的访问控制。那就是用身份，最简单的，就是定义角色像KEY一样来，由于这个是后台控制
 *  所以用THEYLEAF, 用户身份会不会有好几个？有可能的，那为了简单，应该放在一个列表中，然后在控制上，也是一个列表，二个之间有个对照
 *  THEYLEAF好像没这么复杂的表达方式
 * 
 *  那就每个卡片页准备一个键值对，来判断
 *  r.manager ? 这个会在论文投稿成功时，即插入，而且角色名中也没有这个同权限的自定义角色
 *  r.prereview          //有权限决定是否拒稿，以及发送审稿
 *  r.review.authorityeditor    //授权的编辑，授权来源，有权限的编辑邀请的时候赋权，这个权限在投稿的时候决定
 *  r.review.responsingeditor   //响应编辑，也就是SECTION创建时负责的编辑, 有权限邀请编辑，但不一定有权限决定
 *  r.review.editor             //普通编辑，无权的，被邀请
 *  r.author                    //普通作者
 *  r.prereview    //有权决定是否拒稿，以及发送查重，这个不管是REVIEW还是SIMILAR CHECK都是一个样的
 *  r.similarcheck      //有权决定查重
 *  r.playment         //有权处理付款
 * 
 *  现在最大的问题是自定义角色的问题，不对，应该是判断机制。
 *  实际上，有一个误区在，这些角色都是在流程中慢慢插入的，插入的时候，并不是通过期刊TEAM来配置的，而是在流程中赋值，因此
 *  ********＝＝＝－－－－－－－－
 *  只要保证流程插入时的角色是系统预定的就行，不会出问题，所以判断也很容易，REVIEW就是SECTION EDITOR，区别就在于有没有权限
 *  但是原生的SECTION EDITOR就不知道了，从这个记录中看不出来。这个只能从后台数据库中去查
 *  也就是， 如果是MANAGEER ,按UID来查，应该有ROLD_ID =1 ,如果是SECTION EDITOR，按UID来查应该有ROLE_ID=5
 *  再查AID对应的SECTION EDITOR，是不是本人，如果是那就是REPSOING EDITOR，如果不是就是普通编辑 至少有没有权，看DCISION字段
 *  如果是PREIVEWER, ROLE_ID就应该是11，如果是SIMILARCHER, 就应该是10，
 *  如果查不到，就是AUTHOR，因为他能看到，这个最好也要能判断一下
 * 
 *  比较麻烦的是 PRE-REVIEW，可能在REVIEW和SIMILAR CHECK中都出现，这时只能再通过HISTROY记录来判断是在哪个卡片页
 */

@Component
public class ArticleUserRoleHelper {
    @Autowired
    private CallStub callStub;

    @Autowired
    private HistoryHelper helper;

    public JSONObject getEditorListBoard(long pid, long uid){
        return callStub.callStub("editorroles", JSONObject.class, "pid", pid, "uid", uid);
    }

    //获得一篇文章所有的编辑团队，以及他们的角色,主要返回他们的ID, NAME, EMAIL,有没有决定权，是不是响应编辑
    //主要的角色是：主管，编辑（有权，无权，响应编辑），查重编辑，支付编辑，排版编辑
    //role,decision,define,name,email
    public List<JSONObject> getBoard(long aid){
        JSONArray arr = JSONArray.parseArray( callStub.callStub("getBoardBydAid", String.class, "aid", aid));

        System.out.println(arr);
        List<JSONObject> rst = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            rst.add( arr.getJSONObject(i) );
        }
        return rst;
    }

    private boolean assit( HashMap<String, Boolean> userRoles, String key){
        if( userRoles.get(key) == null ) return false;
        return userRoles.get(key);
    }

    //拿一个用户对一篇文章可以有的卡片页
    public boolean getTabsForUser(long aid, HashMap<String, Boolean> userRoles, String tab){
        if(tab.equals("HISTORY")) return true;
        if(tab.equals("SUBMIT")) return true;
        
        if( assit(userRoles,"manager") == true ) return true;//主管都可以
        if( assit(userRoles,"author") == true ) return true;//作者都可以
        if( assit(userRoles,"authorityeditor") == true ||  assit(userRoles,"editor") == true ){
            if( tab.equals("REVIEW")) return true;
        }

        if( assit(userRoles,"prereview") == true){//是预审
            String prereviewInTab = previewInWorkFlow( aid );//预审所在的TAB页
            if( "".equals(prereviewInTab) ){
                return false;
            }else{
                if(tab.equals(prereviewInTab)) return true;
            }
        }
        if(assit(userRoles,"similarcheck") && tab.equals("SIMILARCHECK")) return true;
        if(assit(userRoles,"copyeditor") && tab.equals("COPYEDIT")) return true;
        if(assit(userRoles,"financial") && tab.equals("PAYMENT")) return true;

        return false;
    }

    //拿一个用户对于某篇文章的所有权限
    public HashMap<String, Boolean>  getRigthByUidAndAid(long aid, long uid){
        JSONObject obj = callStub.callStub("getrolesforuser", JSONObject.class, "aid", aid, "uid", uid);
        HashMap<String, Boolean> rst = new HashMap<String, Boolean>();
        rst.put("author", false);
        rst.put("manager", false);
        rst.put("authorityeditor", false);
        rst.put("responsingeditor", false);
        rst.put("authorityeditor", false);
        rst.put("editor", false);
        rst.put("prereview", false);
        rst.put("similarcheck", false);
        rst.put("financial", false);
        rst.put("copyeditor", false);

        //是不是MANAGER;
        rst.put("manager", obj.getBoolean("manager") == null ? false: obj.getBoolean("manager"));
        if( rst.get("manager") ){ //MANAGER
            rst.put("authorityeditor", true); //manager拥有所有权限
        }
        //是不是作者
        rst.put("author", obj.getBoolean("r.author"));
        //在这个论文TEAM中承担的角色
        JSONArray arr = obj.getJSONArray("boards");

        for(int i=0; i<arr.size(); i++){
            JSONObject tmp = arr.getJSONObject(i);

            if( tmp.getLong("roleId") == 5 ){ //SECTION EDITOR
                //是不是期刊栏目责任编辑，责任编辑不一定在此时的团队中，因为责任编辑可能会回避投稿
                rst.put("responsingeditor", obj.getBoolean("r.review.responsingeditor"));
                if( tmp.getBoolean("decision") ){   //authority editor
                    rst.put("authorityeditor", true); 
                }else{
                    rst.put("editor", true);        //normal editor
                }
            }else if( tmp.getLong("roleId") == 11 ){    //PREVIEWER, 预审编辑有决定权
                rst.put("prereview", true);
                //rst.put("authorityeditor", true);
            }else if( tmp.getLong("roleId") == 10 ){    //SIMILAR CHECKER, 查重编辑有决定权
                rst.put("similarcheck", true);
                //rst.put("authorityeditor", true);
            }else if( tmp.getLong("roleId") == 7 ){    //SIMILAR CHECKER, 查重编辑有决定权
                rst.put("financial", true);
            }else if( tmp.getLong("roleId") == 9 ){    //SIMILAR CHECKER, 查重编辑有决定权
                rst.put("copyeditor", true);
            }
        }
        return rst;
    }

    public void saveEditor(long aid, long rid, long uid, boolean decision){
        callStub.callStub("articleboardsave", String.class, "aid", aid, "rid",  rid, "uid", uid, "decision", decision);
    }

    public void delEditor(long aid, long rid, String email){
        callStub.callStub("articleboarddel", String.class, "aid", aid, "rid",  rid, "email", email);
    }

    public void saveEditors(long aid, long rid, List<User> users, boolean decision){
        for(User u: users){
            callStub.callStub("articleboardsave", String.class, "aid", aid, "rid",  rid, "uid", u.getUserId(), "decision", decision);
        }
    }

    //判断预审在哪个卡片页，返回的是卡片页对应的工作流
    public String previewInWorkFlow(long aid){
        return helper.getWorkflowHasPreview( aid );
    }
}
