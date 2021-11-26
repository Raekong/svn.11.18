package edu.nuist.ojs.middle.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.region.Region;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class COSFileDownloader {
    @Value("${uploader.cos.bucketName}")
    private String bucketName;

    @Value("${uploader.cos.secretId}")
    private String accessKeyId;

    @Value("${uploader.cos.secretKey}")
    private String accessKeySecret;

    @Value("${uploader.cos.region}")
    private String endpoint;


    public void exec(String appends, HttpServletRequest request, HttpServletResponse response){

        String[]appendList=appends.split(";");

        try {
            COSCredentials cred = new BasicCOSCredentials(accessKeyId, accessKeySecret);
            Region region = new Region(endpoint);
            ClientConfig clientConfig = new ClientConfig(region);
            COSClient cosClient = new COSClient(cred,clientConfig);

            String fileName = "Zip.zip";
            File zipFile = File.createTempFile(fileName, ".zip");
            FileOutputStream f = new FileOutputStream(zipFile);

            CheckedOutputStream csum = new CheckedOutputStream(f, new Adler32());
            ZipOutputStream zos = new ZipOutputStream(csum);

            for (String append : appendList) {
                String innerId=append.split(",")[1];
                String originName=append.split(",")[0];
                COSObject cosObject = cosClient.getObject(bucketName,innerId);
                InputStream inputStream = cosObject.getObjectContent();
                zos.putNextEntry(new ZipEntry(originName));
                int bytesRead = 0;
                while((bytesRead=inputStream.read())!=-1){
                    zos.write(bytesRead);
                }
                inputStream.close();
                zos.closeEntry(); // 当前文件写完，定位为写入下一条项目
            }
            zos.close();
            String header = request.getHeader("User-Agent").toUpperCase();
            if (header.contains("MSIE") || header.contains("TRIDENT") || header.contains("EDGE")) {
                fileName = URLEncoder.encode(fileName, "utf-8");
                fileName = fileName.replace("+", "%20"); //IE下载文件名空格变+号问题
            } else {
                fileName = new String(fileName.getBytes(), "ISO8859-1");
            }
            response.reset();
            response.setContentType("text/plain");
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Location", fileName);
            response.setHeader("Cache-Control", "max-age=0");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            FileInputStream fis = new FileInputStream(zipFile);
            BufferedInputStream buff = new BufferedInputStream(fis);
            BufferedOutputStream out=new BufferedOutputStream(response.getOutputStream());
            byte[] car=new byte[1024];
            int l=0;
            while (l < zipFile.length()) {
                int j = buff.read(car, 0, 1024);
                l += j;
                out.write(car, 0, j);
            }
            fis.close();
            buff.close();
            out.close();

            cosClient.shutdown();
            zipFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
