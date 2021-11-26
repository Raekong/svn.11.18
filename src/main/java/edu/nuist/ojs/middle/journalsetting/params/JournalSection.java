package edu.nuist.ojs.middle.journalsetting.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class JournalSection {
    private long id;
    private long journalId;
    private String title;
    private String guid;
    private double order;
    private String sectionEditor; //自动分配指定的处理编辑，论文将在初审之后，第一个发给他
    private boolean isAuthority;  //指定这个编辑是否有权拒接和接收论文
    private String expireDay; //section失效的日期，永不失效，则为 2099－12－31
    private boolean open;    //是否开放允许投稿
}
