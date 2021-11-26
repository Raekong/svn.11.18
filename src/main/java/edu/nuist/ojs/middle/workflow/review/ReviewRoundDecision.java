package edu.nuist.ojs.middle.workflow.review;

public enum ReviewRoundDecision {
    ACCEPT("Accept", "接收", 3),
    DECLINE("Decline", "拒搞", 4),
	REVISION("Revision", "修订", 5),
	CHANGE("Change Jouranl", "转刊", 6);

    private int index;
    private String zh;
    private String en;

    private ReviewRoundDecision(String en, String zh, int index) {
        this.en = en;
        this.zh = zh;
        this.index = index;
    }

    public String getTitle(boolean isZh){
        return isZh ? this.zh :this.en;
    }

    public int getIndex(){
        return this.index;
    }

    public static ReviewRoundDecision getByIndex(int index) {  
        for (ReviewRoundDecision c : ReviewRoundDecision.values()) {  
            if (c.getIndex() == index) {  
                return c;
            }  
        }  
        return null;  
    }
}
