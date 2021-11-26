package edu.nuist.ojs.middle.journalsetting.params;

import edu.nuist.ojs.common.entity.journalsetting.JournalEmailTplSetting;
import edu.nuist.ojs.common.entity.journalsetting.JournalSettingParam;
import lombok.Data;

import java.util.List;

@Data
public class JournalEmailSetting extends JournalSettingParam{
    private List<JournalEmailTplSetting> tpls;

    public JournalEmailTplSetting getDefault(){
        for(JournalEmailTplSetting tpl : tpls){
            if( tpl.isDefault() ){
                return tpl;
            }
        }
        return null;
    }
}
