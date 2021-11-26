package edu.nuist.ojs.middle.file;

import com.alibaba.fastjson.JSONObject;
import com.tencent.cloud.CosStsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 腾讯云上传Uploader
 */
@Component
public class COSFileUploader implements FileUploader {
    @Value("${uploader.cos.bucketName}")
    private String bucketName;

    @Value("${uploader.cos.secretId}")
    private String secretId;

    @Value("${uploader.cos.secretKey}")
    private String secretKey;

    @Value("${uploader.cos.region}")
    private String region;

    @Override
    public Map<String, String> uploadSign(  ) {
        String finurl = getSign( );
        Map<String, String> rst = new HashMap<>();
        rst.put("finalUrl", finurl);
        return rst;
    }

    private String getSign(){
            TreeMap<String, Object> config = new TreeMap<>();
            try {
                config.put("secretId", secretId);
                config.put("secretKey", secretKey);
                config.put("durationSeconds", 600);
    
                config.put("bucket", bucketName);
                config.put("region", region);
                config.put("allowPrefix", "*");
    
                String[] allowActions = new String[]{
                        "name/cos:PutObject",
                        "name/cos:PostObject"
                };

                config.put("allowActions", allowActions);
                JSONObject credential = CosStsClient.getCredential(config);
                credential.put("bucket", bucketName);
                credential.put("region", region);
                
                return credential.toString();
            } catch (Exception e) {
                // 请求失败，抛出异常
                throw new IllegalArgumentException("no valid secret !");
            }
    }

    
}
