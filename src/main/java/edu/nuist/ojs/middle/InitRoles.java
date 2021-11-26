package edu.nuist.ojs.middle;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import edu.nuist.ojs.common.entity.Role;
import edu.nuist.ojs.common.entity.journalsetting.JournalRole;
import edu.nuist.ojs.common.entity.journalsetting.JournalRoleEnum;
import edu.nuist.ojs.middle.stub.CallStub;

@Configuration
public class InitRoles {
    @Autowired
    private CallStub callStub;

    @PostConstruct
	public void addSuperAdmin() {
        JournalRole r = JournalRoleEnum.MANAGER;
        Role test = callStub.callStub("findRoleByAbbr", Role.class, "abbr", r.getAbbr());
        if( test != null && test.getId() != 0){
            return;
        }

        for(JournalRole role: JournalRoleEnum.getAllOriginRolesForJournal()){
            callStub.callStub("saveRole", Role.class,  "role", role);
        }


    }
}
