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

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OOSFileDownloader {
    
    @Value("${uploader.oos.bucketName}")
    private String bucketName;

    @Value("${uploader.oos.accessKey}")
    private String accessKeyId;

    @Value("${uploader.oos.secretKey}")
    private String accessKeySecret;

    @Value("${uploader.oos.region}")
    private String endpoint;

    public void exec( String appends, HttpServletRequest request, HttpServletResponse response){
        String[]appendList=appends.split(";");
        String key = null;
        try{
            OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);;
            String fileName = "Zip.zip";
            File zipFile = File.createTempFile(fileName, ".zip");
            FileOutputStream f = new FileOutputStream(zipFile);
            CheckedOutputStream csum = new CheckedOutputStream(f, new Adler32());
            ZipOutputStream zos = new ZipOutputStream(csum);
            for (String append : appendList) { 
                String innerId=append.split(",")[1];
                String originName=append.split(",")[0];
                OSSObject ossObject = ossClient.getObject(bucketName, innerId);
                InputStream inputStream = ossObject.getObjectContent();
 
                zos.putNextEntry(new ZipEntry(originName));
                int bytesRead = 0;
                while ((bytesRead = inputStream.read()) != -1) {
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
            BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
            byte[] car = new byte[1024];
            int l = 0;
            while (l < zipFile.length()) {
                int j = buff.read(car, 0, 1024);
                l += j;
                out.write(car, 0, j);
            }
            fis.close();
            buff.close();
            out.close();

            ossClient.shutdown();
            zipFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
