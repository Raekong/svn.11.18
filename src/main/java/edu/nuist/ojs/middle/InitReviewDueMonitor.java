package edu.nuist.ojs.middle;


import java.text.SimpleDateFormat;
import java.util.Date;

import com.alibaba.fastjson.JSONArray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import edu.nuist.ojs.middle.datamonitor.MonitorDataAssembly;
import edu.nuist.ojs.middle.stub.CallStub;
import edu.nuist.ojs.middle.workflow.WorkflowMailHelper;


@Configuration      
@EnableScheduling   
public class InitReviewDueMonitor {
    @Autowired
    private CallStub callStub;

    @Autowired
    private WorkflowMailHelper mailHelper; 

    @Scheduled(cron = "0 */2 * * * ?")
    private void configureTasks() {
        SimpleDateFormat ww = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        String date = ww.format(new Date());
        System.out.println("now scanning review action's due ................");
        

        callStub.callStub("updatereviewoverdue", String.class, "date", date);


        String raids = callStub.callStub("needRemind", String.class);
        JSONArray raidArr = JSONArray.parseArray(raids);
        for(int i=0; i<raidArr.size(); i++){
            long raid = raidArr.getLongValue(i);
            mailHelper.sendReviewMail(raid, "Review Remind");
            callStub.callStub("updateRemindCount", String.class, "raid", raid, "isSystem", true);
        }
    } 
}
