package edu.nuist.ojs.middle.datamonitor.datamonitormap;

import java.util.List;

import lombok.Data;

@Data
public class MonitorQuery {
    private int index;
    private String title;
    private String titlequery;
    private boolean showDefault;
    private List<String> query; 
    private String size;
}
