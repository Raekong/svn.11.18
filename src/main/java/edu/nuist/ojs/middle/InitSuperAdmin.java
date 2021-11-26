package edu.nuist.ojs.middle;


import edu.nuist.ojs.common.entity.I18N;
import edu.nuist.ojs.common.entity.User;
import edu.nuist.ojs.middle.stub.CallStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class InitSuperAdmin {

    @Autowired
    private CallStub callStub;
	
	@Value("${global.superadmin.email}")
	private String superEmail;

	@Value("${global.superadmin.password}")
	private String password;

	@Value("${global.superadmin.firstname}")
	private String name;
	
	@PostConstruct
	public void addSuperAdmin() {
		if(  callStub.isSuper(superEmail) == null){
            User u = new User();
			u.setEmail(superEmail);
			u.setPassword(password);
			u.setSuperUser(true);
			u.setI18n(I18N.EN);
			u.setFirstname(name);
			u.setPublisherId(-1);
			u.setActived(true);
			callStub.regist( u );
        };
	}
}
