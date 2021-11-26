package edu.nuist.ojs.middle.resourcemapper.stub;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import lombok.Data;

@Data
public class ServicePoint {
    private String ip;
    private String router;
    private HashMap<String, String> stub = new HashMap<>();

    public List<String> isMatch(String service){
        
        List<String> rst = new LinkedList<>();
        for(Entry<String, String> e: stub.entrySet()){
            if( e.getKey().equals(service)){
                rst.add(e.getValue());
            }
        }
        return rst;
    }
}
