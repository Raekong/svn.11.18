package edu.nuist.ojs.middle.controller.article;
import java.sql.Timestamp;
import java.util.HashMap;

import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSONObject;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.nuist.ojs.common.entity.Payment;
import edu.nuist.ojs.common.entity.article.ArticleFile;
import edu.nuist.ojs.middle.controller.ThymleafHelper;
import edu.nuist.ojs.middle.email.MessageComponent;
import edu.nuist.ojs.middle.interceptor.ContextAnnotation;
import edu.nuist.ojs.middle.workflow.ArticleFileHelper;
import edu.nuist.ojs.middle.workflow.ArticleInfoHelper;
import edu.nuist.ojs.middle.workflow.PaymentHelper;
import edu.nuist.ojs.middle.workflow.WorkFlowMainStateMachine;
import edu.nuist.ojs.middle.workflow.payment.PayTabHelper;
import edu.nuist.ojs.middle.workflow.payment.PaymentStateMachine;
import edu.nuist.ojs.middle.workflow.payment.StripeService;

import org.springframework.ui.Model;

@Controller
public class PaymentController {
    @Autowired
    private PaymentHelper pHelper; 

    @Autowired
    private PayTabHelper tabHelper;  

    @Autowired
    private ArticleFileHelper aFileHelper; 

    @Autowired
    private ArticleInfoHelper iHelper;  
    	
	@Autowired
	private StripeService stripeService;

    @Autowired
	private MessageComponent mComponent;
    
    @Autowired
	private WorkFlowMainStateMachine stateMachine;

    @Autowired
	private PaymentStateMachine pMachine;

    @RequestMapping("/payment/extend/{payid}")
    public String extendPay(
        Model model,
        @PathVariable long payid
    ){
        Payment p = pHelper.getPaymentById(payid); 
        HashMap<String, String> infos = iHelper.getVariableMap(p.getAid());
        model.addAttribute("jtitle", infos.get("#Journal Title#"));
        model.addAttribute("pay", p);
        model.addAttribute("aid", infos.get("#Article Id#"));
        model.addAttribute("jid", infos.get("#jid#"));
        model.addAttribute("atitle", infos.get("#Article Title#"));
        model.addAttribute("authors", infos.get("#Contributor Name#"));
        model.addAttribute("pays", tabHelper.getPaymentsByAid(p.getAid()));
        model.addAttribute("bankinfo", tabHelper.getBankInfo(Long.valueOf(infos.get("#jid#"))));
        model.addAttribute("files",  aFileHelper.getPaymentFiles(p.getAid())); 
        return "article/payPage";
    }

    @RequestMapping("/payment/extend")
    public String extendPay(){
        return "article/payPage";
    }

    @RequestMapping("/payment/countAndApc")
    @ResponseBody
    public JSONObject count(@RequestParam int pnum, @RequestParam long  aid, @RequestParam long  jid){
        return pHelper.getAPCInfos(jid, pnum);
    }

    @Value("${payment.stripe.publicKey}")
    private String publicKey;
   
    @Value("${payment.stripe.apiKey}")
    private String apiKey;
    
    @Value("${payment.stripe.webhookSecret}")
    private String webhookSecret;

    //STRIPE收费窗口
    @RequestMapping("/payment/stripe")
    public String stripe (
        Model model,
        HttpSession session,
        @RequestParam long aid, 
        @RequestParam long payid, 
        @RequestParam int  paynum, 
        @RequestParam int  paytotal, 
        @RequestParam boolean  isBack){
            
                HashMap<String, String> infos = iHelper.getVariableMap(aid); 
                model.addAttribute("stripePublicKey", publicKey);
                model.addAttribute("total", paytotal);
                model.addAttribute("aid", aid);
                model.addAttribute("pnum", paynum);
                model.addAttribute("atitle", infos.get("#Article Title#"));
                model.addAttribute("payid", payid);
                String email = null;
                email = ThymleafHelper.get(session, "email", String.class);
                model.addAttribute("email", email);
                model.addAttribute("jid", infos.get("#jid#"));
                model.addAttribute("jtitle", infos.get("#Journal Title#"));
           
        return "article/stripe-pop";
    }

    //STRIPE收费成功
    @RequestMapping("/create-charge")
    @ResponseBody
    public String stripeCharge(
        @RequestParam long aid, 
        @RequestParam long payid, 
        @RequestParam String email,
        @RequestParam String token,
        @RequestParam int pnum,
        @RequestParam int total
    ) {
		// You may want to store charge id along with order information
        Charge charge = stripeService.createCharge(aid, token, total*100);

        if( charge.getStatus().equals("succeeded")) {
            Payment p = pHelper.getPaymentById(payid); 
            p.setPayTotal(total);
            p.setPayType(Payment.ONLINE);
            p.setPayPageNumber(pnum);
            p.setPayEmail(email);
            p.setPaytime(new Timestamp(System.currentTimeMillis()));

            ArticleFile af = ArticleFile
                            .builder()
                            .aid(aid)
                            .innerId(charge.getId())
                            .originName("Article-" + aid + "-Stripe-Receipt.html")
                            .version("PAYMENT-0")
                            .build();
            af = aFileHelper.saveFile(af);

            p.setWireFileId(af.getId());
            pHelper.save(p);
            pMachine.paid(aid, total);
            return "true";
        }else{
            return "false";
        }
        
    }

    //上传WIRE收费凭据,收费成功
    @RequestMapping("/payment/wire")
    @ResponseBody
    public String wire(
        @RequestParam long aid, 
        @RequestParam long payid, 
        @RequestParam int  paynum, 
        @RequestParam int  paytotal, 
        @RequestParam boolean  isBack,
        @RequestParam String  originName,
        @RequestParam String  innerId,
        @RequestParam String  email
          
    ){
        Payment p = pHelper.getPaymentById(payid); 
        p.setPayTotal(paytotal);
        p.setPayType(Payment.WIRE);
        p.setPayPageNumber(paynum);
        p.setPaytime(new Timestamp(System.currentTimeMillis()));
        p.setPayEmail(email);

        String fileType = originName.substring( originName.lastIndexOf("."));
        ArticleFile af = ArticleFile
                        .builder()
                        .aid(aid)
                        .innerId(innerId)
                        .originName("Article-" + aid + "-WireTransfer-Receipt" + fileType)
                        .version("PAYMENT-0")
                        .build();
        af = aFileHelper.saveFile(af);

        p.setWireFileId(af.getId());
        pHelper.save(p);

        pMachine.paid(aid, paytotal);
        return "";
    }

    @RequestMapping("/payment/downreceipt")
    @ResponseBody
    public JSONObject downReceipt(
        @RequestParam long  fid,
        @RequestParam String type
    ){
        ArticleFile f = aFileHelper.getArticleFileById(fid);
        JSONObject obj = new JSONObject();
        if( Payment.ONLINE.equals(type)){
            try {
                String url = stripeService.getRecieptFile(f.getInnerId());
                obj.put("link", url);
                obj.put("fileName", f.getOriginName());
                return obj;
            } catch (StripeException e) {
                e.printStackTrace();
            }
            return null;

        }else if(Payment.WIRE.equals(type)){
            String link = mComponent.getFileUrl(f.getInnerId());
            obj.put("link", link);
            obj.put("fileName", f.getOriginName());
        }

        return obj;
    }

    
    @RequestMapping("/payment/done")
    @ResponseBody
    @ContextAnnotation(configPoint = "stripe", configKeys = "u.userId,u.i18n,p.i18n,u.email,u.root,p.id,p.abbr" )
    public String auditPass(
        HttpSession session,
        @RequestParam long aid,
        @RequestParam long jid
    ){
        long uid = ThymleafHelper.get(session, "userId", long.class);
        pMachine.payAudit(aid, jid, uid);        
        stateMachine.payed( jid, aid );
        return "";
    }

    @RequestMapping("/payment/useremail")
    @ResponseBody
    public String userEmail(
        HttpSession session
    ){
        String email = null;
        email = ThymleafHelper.get(session, "email", String.class);

        return email==null? "":email;
    }

    @RequestMapping("/payment")
    public String paymentForComplie(
        HttpSession session
    ){
        
        return "article/paycompont";
    }

    @RequestMapping("/payment/comp/{aid}")
    @ResponseBody
    public HashMap<String, String> getArticleInfo(@PathVariable long aid){
        if( !pMachine.canPay(aid) ){
            //论文不能支付
            return new HashMap<String, String>();
        }else{
            return iHelper.getVariableMap(aid);
        }
       
    }

    //使用银行凭证补交成功
    @RequestMapping("/payment/wire/back")
    @ResponseBody
    public String payWireBack(
        @RequestParam long aid,  
        @RequestParam String email,
        @RequestParam String innerId,
        @RequestParam String originName,
        @RequestParam int paytotal
    ){

        Payment p = Payment.builder()
                    .aid(aid)
                    .payTotal(paytotal)
                    .payType(Payment.WIRE)
                    .paytime(new Timestamp(System.currentTimeMillis()))
                    .payEmail(email)
                    .isBack(true)
                    .build();

        String fileType = originName.substring( originName.lastIndexOf("."));
        ArticleFile af = ArticleFile
                        .builder()
                        .aid(aid)
                        .innerId(innerId)
                        .originName("Article-" + aid + "-WireTransfer-Receipt" + fileType)
                        .version("PAYMENT-0")
                        .build();
        af = aFileHelper.saveFile(af);

        p.setWireFileId(af.getId());
        pHelper.save(p);

        pMachine.paid(aid, paytotal);
        return "";
    }

    @RequestMapping("/payment/stripe/back")
    public String stripeBackPop(
        @RequestParam long aid,
        @RequestParam int total,
        @RequestParam String email,
        Model model
    ){
        HashMap<String, String> infos = iHelper.getVariableMap(aid); 
        model.addAttribute("stripePublicKey", publicKey);
        model.addAttribute("total", total);
        model.addAttribute("aid", aid);
        model.addAttribute("atitle", infos.get("#Article Title#"));
        model.addAttribute("jid", infos.get("#jid#"));
        model.addAttribute("jtitle", infos.get("#Journal Title#"));
        model.addAttribute("email", email);
        return "article/stripe-pop";
    }

    //使用STRIPE补交成功
    @RequestMapping("/create-charge-back")
    @ResponseBody
    public String stripeBackPaid(
        @RequestParam long aid,
        @RequestParam String token,
        @RequestParam String email,
        @RequestParam int total
    ){
        Charge charge = stripeService.createCharge(aid, token, total*100);

        if( charge.getStatus().equals("succeeded")) {
            Payment p = Payment.builder()
                .aid(aid)
                .payTotal(total)
                .payType(Payment.ONLINE)
                .paytime(new Timestamp(System.currentTimeMillis()))
                .payEmail(email)
                .isBack(true)
                .build();

            ArticleFile af = ArticleFile
                            .builder()
                            .aid(aid)
                            .innerId(charge.getId())
                            .originName("Article-" + aid + "-Stripe-Receipt.html")
                            .version("PAYMENT-0")
                            .build();
            af = aFileHelper.saveFile(af);

            p.setWireFileId(af.getId());
            pHelper.save(p);
            pMachine.paid(aid, total);
            return "true";
        }else{
            return "false";
        }
    }

   
}
