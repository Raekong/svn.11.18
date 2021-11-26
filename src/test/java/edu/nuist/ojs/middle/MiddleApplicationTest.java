package edu.nuist.ojs.middle;


import edu.nuist.ojs.common.entity.EmailConfigPoint;
import edu.nuist.ojs.common.entity.EmailTpl;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.Publisher;
import edu.nuist.ojs.common.entity.SimilarCheck;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.ArticleUserRoleHelper;
import edu.nuist.ojs.middle.workflow.HistoryHelper;
import edu.nuist.ojs.middle.workflow.JournalUserRoleHelpler;
import edu.nuist.ojs.middle.workflow.WorkFlowMainStateMachine;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;
import edu.nuist.ojs.middle.workflow.review.ReviewRound;
import edu.nuist.ojs.middle.workflow.review.ReviewTabHelper;
import edu.nuist.ojs.middle.workflow.similarcheck.SimilarCheckTabHelper;
import edu.nuist.ojs.middle.workflow.submit.PaperAnlysis;
import edu.nuist.ojs.middle.workflow.submit.StateMachine;

import org.springframework.test.context.junit4.SpringRunner;

import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.common.entity.journalsetting.JournalConfigPointEnum;
import edu.nuist.ojs.common.entity.journalsetting.JournalSetting;
import edu.nuist.ojs.middle.context.Context;
import edu.nuist.ojs.middle.controller.article.ArticleListBoardController;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.email.EmailConfig;
import edu.nuist.ojs.middle.email.MessageHelper;
import edu.nuist.ojs.middle.email.MessageTplComponent;
import edu.nuist.ojs.middle.journalsetting.JournalSettingHelper;
import edu.nuist.ojs.middle.resourcemapper.emailtpl.EmailTplMapper;
import edu.nuist.ojs.middle.resourcemapper.emailtpl.EmailTplVariableMapper;
import edu.nuist.ojs.middle.resourcemapper.i18n.I18N;
import edu.nuist.ojs.middle.resourcemapper.i18n.component.UploaderInfo;
import edu.nuist.ojs.middle.resourcemapper.journal.JournalConfig;
import edu.nuist.ojs.middle.resourcemapper.modules.Modules;
import edu.nuist.ojs.middle.resourcemapper.stub.ServiceMap;
import edu.nuist.ojs.middle.stub.CallServiceUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.websocket.MessageHandler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.styledxmlparser.css.media.MediaDeviceDescription;
import com.itextpdf.styledxmlparser.css.media.MediaType;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;

import org.apache.tika.exception.TikaException;
import org.apache.xerces.util.URI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@RunWith(SpringRunner.class)
@SpringBootTest

public class MiddleApplicationTest {
	
	@Autowired
	private UploaderInfo uInfos;

	@Autowired
	private ServiceMap sMap;

	@Autowired
	private CallStub callStub;

	@Autowired
	CallServiceUtil callUtil;
	

	@Autowired
	Modules modules;

	@Autowired
	JournalSettingHelper jsh;

	
	@Autowired
	MessageComponent eComponent;

	@Autowired
	EmailTplMapper emailTplMapper;

	@Autowired
	JournalConfig jConfig;

	@Autowired
	EmailTplVariableMapper emailTplVariableMapper;
	
	@Autowired
	ArticleListBoardController bcontroller;

	@Test
	public void v(){

		System.out.println( bcontroller.getUserShowSetting(9) );
	}


	//@Test
	public void t() {
		// User u = User.builder().userId(2).email("284615715@qq.com").firstname("han jin").build();
		// //eComponent.SendPublishMessage(true, 1, u, "test", "Test", "A Test Email for only test", null );
		// eComponent.SendJournalMessage(true, 2, u, "test", "Test", "A Test Email for only test", null );
		
		//String users = callStub.callStub("getUserByEmailAndPid", String.class, "email", "google", "pid", 1);
		//System.out.println(users);	
		//List rst = callStub.callStub("queryteam", List.class, "jid", 1);

		HashMap<String, String> params = new HashMap<>();
        params.put("#username#", "han");
        params.put("#email#", "284615715");
        params.put("#password#", "7539510");
        params.put("#publisheraddress#", "fafasf.........");
        params.put("#publishername#", "fafasf.........");
        params.put("#link#", "fafasf.........");

		EmailTpl tpl = emailTplMapper.getSystemTpl("Account Confirm");

		String content = tpl.renderTitle(params, true);
		System.out.println( content );

	}

	@Autowired
	private PaperAnlysis paperAnlysis;

	
	@Autowired
	private WorkflowMailHelper mailHelper;

	@Autowired
	private ArticleInfoHelper infoHelper; 

	    
    @Autowired
    private StateMachine stateMachine; 

	@Autowired
	MessageTplComponent mTplComponent;
	
	@Autowired
	ArticleFileHelper aHelper;

	@Autowired
	HistoryHelper helper;

	@Autowired
	ReviewTabHelper rhelper;

    @Autowired
    private JournalUserRoleHelpler jrHelper; 

	@Autowired
    private ArticleUserRoleHelper rHelper; 

	@Autowired
    private ReviewTabHelper rtHelper; 

	@Autowired
    private WorkFlowMainStateMachine flowMachine;

	@Autowired
    private SimilarCheckTabHelper scHelper; 

	//@Test
	public void statmachine(){
		// String rst = callStub.callStub("getJournalEditorTeamByRole", String.class, "jid", 1, "rid", 7); 
		// System.out.println(rst);
		// HashMap<String, String> infos = infoHelper.getVariableMap(1l, null, null, 1l);
		// System.out.println(infos.get("#pi18n#"));

		// boolean isEmail = emailTplMapper.getEmailConfigPoint("Submission Ack").isEmail();
		// JSONObject obj = new JSONObject();
		// mTplComponent.getDefaultTpl(1l, "Submission Ack", obj);
		// System.out.println(obj);

		//aHelper.serizalFileFromLastStatus( helper.getHistoryById(2));
		
		// System.out.println( callStub.callStub("getBoardBydAid", String.class, "aid", 4l) );

		// System.out.println(rHelper.getRigthByUidAndAid(4l, 10l));
		//System.out.println(rtHelper.getRounds(10, false));
		// HashMap<String, JournalSetting> rst = flowMachine.prepare(2l);
		// System.out.println(rst.get("Technical Check") == null? "false" : rst.get("Technical Check").getConfigContent());
		// String msgJson = callStub.callStub("getDiscuss", String.class, "aid", 1, "rid", 0);
        // JSONArray arr = JSONArray.parseArray(msgJson);
		// System.out.println(arr.size());

		// String rts = callStub.callStub("findSimilarCheckByAidAndRound", String.class, "aid", 5, "round", 1);
		// SimilarCheck sc = JSONObject.toJavaObject( JSONObject.parseObject(rts), SimilarCheck.class);  
		// System.out.println( rts );
		// System.out.println( scHelper.isPassed(sc) ); 

		// @RequestAttribute String name,
        // @RequestAttribute String email,
        // @RequestAttribute String affiliation,
        // @RequestAttribute String research,
        // @RequestAttribute long pid,
        // @RequestAttribute int  page,
        // @RequestAttribute int size
		
		// String rts = callStub.callStub(
		// 	"queryReviewer", 
		// 	String.class, 
		// 	"name", "", 
		// 	"email", "4", 
		// 	"affiliation", "",  
		// 	"research", "",  
		// 	"pid", 1, 
		// 	"page", 1, 
		// 	"size", 4
		// );

		// System.out.println( rts ); 
		Stripe.apiKey = "sk_test_Ou1w6LVt3zmVipDVJsvMeQsc";

		try {
			Charge charge = Charge.retrieve("ch_3JbZqlJnvmXwwenz1DXnCesB");
			System.out.println( charge.getReceiptUrl() ) ;
		} catch (StripeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//@Test
	public void printfPDF() throws MalformedURLException, FileNotFoundException, IOException{
		String url = "https://pay.stripe.com/receipts/acct_162FRyJnvmXwwenz/ch_3JbZqlJnvmXwwenz1DXnCesB/rcpt_KG62Tx0M8RorP1HehhoSU45IJ9NhBlP";
		
		ConverterProperties properties = new ConverterProperties();
		MediaDeviceDescription mediaDeviceDescription = new MediaDeviceDescription(MediaType.PRINT);
		properties.setMediaDeviceDescription(mediaDeviceDescription);
		HtmlConverter.convertToPdf(new URL(url).openStream(), new FileOutputStream("d:/test.pdf"), properties); 
	}

	//@Test
	public void nacos(){
		JSONObject obj = new JSONObject();
		obj.put("p", 1);
		obj.put("j", 1);
		obj.put("js", 1);
		obj.put("a", 1);

		//mailHelper.sendArticleProcessMessage(obj, 1, 1, I18N.EN, "Submission Ack", null);
		//System.out.println(mailHelper.getVariableMap(1l, 1l, 1l,  1l));
		//System.out.println( emailTplVariableMapper.getTplvariables().get("journalteam").keySet());
		// String obj = callStub.callStub("getAllEmailConfigForJournal", String.class, "jid", 2);
		// JSONArray arr = JSONObject.parseArray(obj);

        // List<EmailConfigPoint> tmp = new LinkedList<>();
        // for(int i=0; i<arr.size(); i++){	
        //     JSONArray t =  arr.getJSONObject(i).getJSONArray("tpls") ;
		// 	for(int j=0; j<t.size(); j++){
		// 		System.out.println( EmailTpl.get(t.getJSONObject(j).getJSONObject("jsonData")));
		// 	}
        // }

		// String url = "https://hnajin-1306618516.cos.ap-nanjing.myqcloud.com/0b807b99-6bd9-4a53-a9d0-fea9f19e14f6.docx";
		// try {
		// 	JSONObject obj = paperAnlysis.getArticleParams(url, "pdf");
		// 	System.out.println(obj);
		// } catch (IOException | TikaException e) {
		// 	// TODO Auto-generated catch block
		// 	e.printStackTrace();
		// }
		
		//System.out.println(jConfig.getConfigs().size());

		// Message m = callStub.getMessage(6, "Account Confirm");
		// System.out.println( m.getContent() );
		// // JSONObject data = new JSONObject();

		// System.out.println(data);
		//String rst = callUtil.callService("publisher", "test", data , String.class);

		//test isSuper
		//data.put("email", "hjhaohj@126.com");
		//String rst = callUtil.callService("publisher", "isSuper", data , String.class);
		//System.out.println(rst);

		//test regist
//		User u = new User();
//		u.setEmail("1191318321@qq.com");
//		u.setPassword("ttt");
//
//		Map<String, Object> param = new HashMap<>();
//		param.put("user", u);
//		param.put("publisherAbbr","admin");
//		String rst = callUtil.callService("publisher", "/user/regist", param , String.class);
//		System.out.println(rst);
//		Map<String,Object>map=callStub.resetPassword("admin","1191318321@qq.com");
//		System.out.println(map);
//		if(map.get("flag").toString().equals("true")){
////			callStub.sendEmail("1191318321@qq.com",map.get("code").toString());
//			for(int i=0;i<10;i++)
//				callStub.sendEmail("1191318321@qq.com",map.get("code").toString());
//			System.out.println("true");
//		}
//		System.out.println(callStub.login("1191318321@qq.com","919072",-1));
//		Map<String,Object> param=new HashMap<String, Object>();
//		param.put("fileName","Log1.csv");
//		callUtil.callService("email","/upload",param,String.class);
//		Publisher publisher= Publisher.builder()
//				.abbr("21daojhf")
//				.contact("zzz")
//				.disable(false)
//				.email("2232")
//				.emailSender("zhj")
//				.i18n("")
//				.build();
//		User user = User.builder()
//				.email("zhj123")
//				.username("zhj123")
//				.password("123")
//				.build();
//		System.out.println(callStub.newPublisher(publisher,user));

//
//
//		System.out.println(callStub.disable(1));
//		System.out.println(callStub.searchPublishers(1,3,"q",null));
//				Publisher publisher= Publisher.builder()
//						.id(2)
//				.abbr("21daojhf")
//				.contact("6666666666")
//				.disable(false)
//				.email("2232")
//				.emailSender("zhj")
//				.i18n("")
//				.build();
//		System.out.println(callStub.updatePublisher(publisher));
	}
	

	@Test
	public void ss(){
		long d =2;
		
	}
	
}
