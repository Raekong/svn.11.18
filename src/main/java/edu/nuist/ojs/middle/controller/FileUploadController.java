package edu.nuist.ojs.middle.controller;

import com.alibaba.fastjson.JSONObject;
import edu.nuist.ojs.middle.file.COSFileUploader;
import edu.nuist.ojs.middle.file.LocalFileUploader;
import edu.nuist.ojs.middle.file.OOSFileUploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@RestController
public class FileUploadController {
    @Autowired
    private LocalFileUploader localUploader;

    @Autowired
    private COSFileUploader cosUploader;

    @Autowired
    private OOSFileUploader oosFileUploader;

    /**
     * OOS , 阿里云上传，先拿到SIGN，再去上传，阿里云会直接回调链接，调回OOSFileUploader.CALLBACK_URL
     * 这个是写死在里面的，完成上传后其它事项
     * @return
     */
    @RequestMapping("/file/uploader/oos/sign")
    public Map<String, String> oosSign(){
        return oosFileUploader.uploadSign();
    }
    //OOS回调，我们直接返回回调得到的参数
    @RequestMapping( OOSFileUploader.CALLBACK_URL )
    public Map<String, String> oosCallback(  @RequestParam Map<String, String> params ){
        return params;
    }

    /***
     * 腾讯云上传约定使用POST方式，因此仅需要一个参数，就是本地文件的路径
     * 腾讯云就是上传，没有回调，需要前端在接收后发送回调，完成后继操作
     */
    @RequestMapping("/file/uploader/cos/sign")
    public JSONObject cosSign(
    ){
        return JSONObject.parseObject( cosUploader.uploadSign( ).get("finalUrl"));
    }

    //本地上传组件，返回的是空，就是做个样子
    @RequestMapping("/file/uploader/local/sign")
    public Map<String, String> localSign(
    ){
        return localUploader.uploadSign(  );
    }

    /**
     *  本地上传组件，上传操作，上传之后，如果正常会返回
     *  origin：上传文件的原始名
     *  inner：上传文件的随机名
     *  flag：是否正常，true正常上传，false 上传出错
     */ 
    @RequestMapping("/file/uploader/local/upload")
	public void localUpload(
			@RequestParam MultipartFile file,
            @RequestParam String uuid,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException {
		JSONObject obj = localUploader.upload(uuid, file);
        PrintWriter out = response.getWriter(); 
        out.append(obj.toJSONString());
	}

}
