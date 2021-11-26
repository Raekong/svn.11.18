package edu.nuist.ojs.middle.file;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云上传Uploader
 */
@Component
public class OOSFileUploader implements FileUploader {

    @Value("${uploader.oos.bucketName}")
    private String bucketName;

    @Value("${uploader.oos.accessKey}")
    private String accessKey;

    @Value("${uploader.oos.secretKey}")
    private String secretKey;

    @Value("${uploader.oos.region}")
    private String region;

    private String dir = UUID.randomUUID().toString();

    public static final String CALLBACK_URL = "http://47.101.39.135:8080/file/upload/oss/callback";

    @Override
    public Map<String, String> uploadSign() {
        String host = "http://" + bucketName + "." + region; 
        
        Map<String, String> respMap = new LinkedHashMap<String, String>();
        OSS client = new OSSClientBuilder().build(region, accessKey, secretKey);
        try {
            long expireTime = 100; //官方默认是 30秒 这里会出现 缓存的问题 解决 验证签名时会出现问题
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            
            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

            String postPolicy = client.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = client.calculatePostSignature(postPolicy);

            respMap.put("accessid", accessKey);
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", dir);
            respMap.put("host", host);
            respMap.put("expire", String.valueOf(expireEndTime / 1000));

            JSONObject jasonCallback = new JSONObject();
            jasonCallback.put("callbackUrl", CALLBACK_URL);
            jasonCallback.put("callbackBody",
                    "filename=${object}&size=${size}&mimeType=${mimeType}&height=${imageInfo.height}&width=${imageInfo.width}");
            jasonCallback.put("callbackBodyType", "application/json");
            String base64CallbackBody = BinaryUtil.toBase64String(jasonCallback.toString().getBytes());

            respMap.put("callback", base64CallbackBody);
            return  respMap;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return  respMap;
    }

}
