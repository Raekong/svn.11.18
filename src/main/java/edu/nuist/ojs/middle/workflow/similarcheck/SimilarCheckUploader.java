package edu.nuist.ojs.middle.workflow.similarcheck;


import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.nuist.ojs.common.entity.SimilarCheck;
import edu.nuist.ojs.middle.stub.CallServiceUtil;
import edu.nuist.ojs.middle.stub.CallStub;

@Service
public class SimilarCheckUploader {
      
    @Value("${global.simialcheck.folderId}")
    private String folderId;
  
    @Autowired
    CallServiceUtil callServiceUtil;

    @Autowired
    private CallStub callStub;

    @Async
    /**
     * 表明是异步调用
     * 没有返回值
     */
    public void execUploader( SimilarCheck sc ) {
        Map<String,Object> data=new HashMap<String, Object>(); 
        data.put("folderId", folderId);
        data.put("link", sc.getLink());
        data.put("authorFirstName","");
        data.put("authorLastName","");
        data.put("documentTitle",sc.getTitle());
        data.put("fileName", sc.getFileName());
        String documentId= callServiceUtil.callService("report","upload",data,String.class,"reportServerRouter");
        sc.setCheckid(documentId);
        callStub.callStub("saveSimilarCheck", SimilarCheck.class, "similarCheck", sc);
    }


}
