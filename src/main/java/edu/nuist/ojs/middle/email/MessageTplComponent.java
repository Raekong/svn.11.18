package edu.nuist.ojs.middle.email;

import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.nuist.ojs.common.entity.EmailConfigPoint;
import edu.nuist.ojs.common.entity.EmailTpl;
import edu.nuist.ojs.middle.resourcemapper.emailtpl.EmailTplMapper;
import edu.nuist.ojs.middle.stub.CallStub;

@Component
public class MessageTplComponent {
    @Autowired
    private CallStub callStub;

	@Autowired
	public EmailTplMapper systemEmailConfig;
    //isEmail是一个用于返回是否发送邮件的对象，因为模板的配置点上有是否发送邮件的配置，而模板中没有，又要能返回这个配置
    //所以借用这个对象来返回
    public EmailTpl getDefaultTpl(long jid, String configPoint, JSONObject isEmail){
        String obj = callStub.callStub("getEmailConfigForJournal", String.class, "jid", jid, "configPoint", configPoint);
        JSONObject tmp = JSONObject.parseObject(obj);

        if( tmp == null ) {
            if(systemEmailConfig.getEmailConfigPoint(configPoint).isEmail()){
                isEmail.put("isEmail", "");
            } 
            return systemEmailConfig.getSystemTpl(configPoint);
        }
        EmailConfigPoint ecp = new EmailConfigPoint(tmp);
        if( ecp.getDefault() != null ){
            if(ecp.isEmail()) isEmail.put("isEmail", "");
            return ecp.getDefault();
        }
        if(systemEmailConfig.getEmailConfigPoint(configPoint).isEmail()){
            isEmail.put("isEmail", "");
        } 
        return systemEmailConfig.getSystemTpl(configPoint);
    }


}
