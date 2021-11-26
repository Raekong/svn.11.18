package edu.nuist.ojs.middle.email;

import edu.nuist.ojs.common.entity.EmailFile;
import edu.nuist.ojs.common.entity.Message;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.redis.RedisRouter;
import edu.nuist.ojs.middle.stub.CallStub;

import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class MessageComponent {
    @Value("${uploader.local.base}")
    private String filebase;

    @Value("${uploader.platform}")
    private String platform;

    @Value("${uploader.cos.bucketName}")
    private String cosbucketName;

    @Value("${uploader.cos.region}")
    private String cosregion;

    @Value("${uploader.oos.bucketName}")
    private String oosbucketName;

    @Value("${uploader.oos.region}")
    private String oosregion;

    @Autowired
    RedisRouter redisRouter;
    
    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private CallStub callStub;


    public static final String ORIGIN = "originName";
    public static final String INNERID = "innerId";

    public static final String LOCAL = "LOCAL";
    public static final String OSS = "OSS";
    public static final String COS = "COS";

    //bucketName: zhiwindrain
    //accessKey: LTAI5tAy8bxW3LqpdWWxENNM
    //secretKey: Qm7DHBosM6DUKj3tWztPJE3DF5YD73
    //region: oss-cn-shanghai.aliyuncs.com
    //https://hnajin-1306618516.cos.ap-nanjing.myqcloud.com/02294cfa-7cd9-479c-96c5-457d60718f37 
    //https://zhiwindrain.oss-cn-shanghai.aliyuncs.com/"+key;
    //要开放空间

    //从前台传来的JSONOBJECT对象，包括一个原始文件名和一个内部名
    public List<EmailFile> getFiles(JSONArray obj){
        if( obj != null && obj.size()>0 ){
            List<EmailFile> rst = new LinkedList<>();
            for(int i=0; i<obj.size(); i++){
                rst.add(getFile(obj.getJSONObject(i)));
            } 
            return rst;
        }
        return null;
    }

    public String getFileUrl(String innerId){
        String path = "";
        if( platform.equals(COS) ){
            path = "https://" + cosbucketName +".cos."+cosregion+".myqcloud.com/" + innerId;
        }
        if( platform.equals(OSS) ){
            path = "https://" + oosbucketName +"."+oosregion  + innerId;
        }
        return path; 
    }

    public EmailFile getFile(JSONObject obj){ 
        String originFile = obj.getString(ORIGIN);
        String inner = obj.getString(INNERID);

        if( platform.equals(LOCAL) ){
            int sp = originFile.lastIndexOf(".");
            String file = originFile.substring(0, sp);
            String ext =  originFile.substring( sp+1 );
            return EmailFile.builder().path(filebase).fileName(file).fileType(ext).islink(false).build();
        }

        if( platform.equals(COS) ){
            String path = "https://" + cosbucketName +".cos."+cosregion+".myqcloud.com/" + inner;
            return EmailFile.builder().path(path).fileName(originFile).fileType("").islink(true).build();
        }

        if( platform.equals(OSS) ){
            String path = "https://" + oosbucketName +"."+oosregion  + inner;
            return EmailFile.builder().path(path).fileName(originFile).fileType("").islink(true).build();
        }
        return null; 
    }

    
    public Message SendPublishMessage(
        boolean isEmail,
        long pid,
        User recevier,
        String configPoint,
        String title,
        String content,
        List<EmailFile> attachments
    ){
        Message m = messageHelper.getPublisherMessage(isEmail, pid, recevier, configPoint, title, content, attachments);
        return callStub.sendMessage( m );
    }

    public Message resend(Message m){
        return callStub.sendMessage( m );
    }

    public Message SendJournalMessage( 
        boolean isEmail,
        long jid,
        User recevier,
        String configPoint,
        String title,
        String content,
        List<EmailFile> attachments){
            Message m = messageHelper.getJournalMessage(isEmail, jid, recevier, configPoint, title, content, attachments);
            return callStub.sendMessage( m );
            
    }

    public Message SendUserMessage(
        boolean isEmail,
        User u,
        User recevier,
        String configPoint,
        String title,
        String content,
        List<EmailFile> attachments
    ) {
        Message m = messageHelper.getUserMessage(isEmail, u, recevier, configPoint, title, content, attachments);
        return callStub.sendMessage( m );
        
    }
}
