package edu.nuist.ojs.middle.journalsetting.params;

import java.util.List;

import edu.nuist.ojs.common.entity.journalsetting.JournalSettingParam;
import lombok.Data;

@Data
public class JournalSectionSetting extends JournalSettingParam{
    private List<JournalSection> sections;
}
