package edu.nuist.ojs.middle.journalsetting.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalEditor {
    private String email;
    private String name;
    private String researchFile;
    private String affiliation;
}
