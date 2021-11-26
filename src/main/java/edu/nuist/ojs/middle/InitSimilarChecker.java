package edu.nuist.ojs.middle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import edu.nuist.ojs.common.entity.SimilarCheck;
import edu.nuist.ojs.middle.stub.CallServiceUtil;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.similarcheck.SimilarCheckStateMachine;
import edu.nuist.ojs.middle.workflow.similarcheck.SimilarCheckTabHelper;


@Configuration      
@EnableScheduling 
public class InitSimilarChecker {
    
    @Value("${global.simialcheck.username}")
    private String username;

    @Value("${global.simialcheck.password}")
    private String password;

    
    @Value("${global.simialcheck.getReportData}")
    private String getReportData;
    
    @Autowired
    CallStub callStub;    

    @Autowired
    RestTemplate restTemplate; 

    @Autowired
    private CallServiceUtil callServiceUtil;

    @Autowired
    private SimilarCheckTabHelper scHelper; 

    @Autowired
    private SimilarCheckStateMachine machine; 
   
    @Scheduled(fixedRate = 60000)//从数据库寻找已经上传但是未爬取到数据的发送给爬虫
    public void getReport(){
        Map<String,Object> data1=new HashMap<String, Object>(); 
        data1.put("username", username );
        data1.put("password", password);

        callServiceUtil.callService("report","login",data1, String.class,"reportServerRouter");
        List<SimilarCheck> similarChecks= JSONObject.parseArray(callStub.callStub("findUnchecked", String.class), SimilarCheck.class);
        for(SimilarCheck similarCheck : similarChecks){ 
            Map<String,Object> data=new HashMap<String, Object>();
            data.put("documentId",similarCheck.getCheckid());
            String urls = callServiceUtil.callService("report","getReport",data,String.class,"reportServerRouter");
            if (urls.equals("false")) continue;
           
            JSONObject json= JSONObject.parseObject(urls);
            System.out.println(json.get("view_only_url"));
            String result = restTemplate.postForObject(getReportData, json,String.class);
            
            if(result.equals("false")){ //查重返回出错，查重没有正常执行
                similarCheck.setTotalSimilar( "-1" );
                similarCheck.setFrsSimilar( "-1" );
                similarCheck.setSecSimilar( "-1" );
                similarCheck.setThrSimilar( "-1" );
                callStub.callStub("saveSimilarCheck", SimilarCheck.class, "similarCheck", similarCheck);
                machine.checkError( similarCheck );
                return;
            }else{
                String []res=result.split(";");

                if( res[0].endsWith("%")) res[0] = res[0].substring(0, res[0].length()-1);
                if( res[1].endsWith("%")) res[1] = res[1].substring(0, res[1].length()-1);
                if( res[2].endsWith("%")) res[2] = res[2].substring(0, res[2].length()-1);
                if( res[3].endsWith("%")) res[3] = res[3].substring(0, res[3].length()-1);
                similarCheck.setTotalSimilar(res[0]);
                similarCheck.setFrsSimilar(res[1]);
                similarCheck.setSecSimilar(res[2]);
                similarCheck.setThrSimilar(res[3]);
                callStub.callStub("saveSimilarCheck", SimilarCheck.class, "similarCheck", similarCheck);
            }
            

            if( scHelper.isPassed( similarCheck )){//如果通过了
                machine.passCheck( similarCheck );
            }else{//如果没有通过
                machine.unPassCheck( similarCheck );
            }
        }
    } 
}
