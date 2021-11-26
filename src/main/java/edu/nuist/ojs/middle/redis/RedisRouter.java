package edu.nuist.ojs.middle.redis;

import cn.hutool.crypto.SecureUtil;
import edu.nuist.ojs.common.entity.EmailServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class RedisRouter {
    @Autowired
    RedisTemplate<Object, Object> redisTemplate;
    public String save(String keyHead, Object obj){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String key=keyHead+ SecureUtil.md5(df.format(new Date())+ UUID.randomUUID());
        redisTemplate.opsForValue().set(key, obj);
        return key;
    }
    public void update(String key, Object obj){
        redisTemplate.opsForValue().set(key,obj);
    }
    public String saveEmail(String key, EmailServer emailServer){
        redisTemplate.opsForValue().set(key,emailServer);
        return key;
    }
}
