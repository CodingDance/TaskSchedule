package com.yiting.taskschedule.meta;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by hzyiting on 2016/5/26.
 */
public class Export implements Serializable{
    private Map<String, Object> e = new ConcurrentHashMap<String, Object>();

    private String result = "default";

    public Export() {
    }

    public Object get(String key) {
        return e.get(key);
    }

    public void put(String key, Object o) {
        if (o != null) {
            e.put(key, o);
        }
    }

    public Set<String> getKeySet() {
        return e.keySet();
    }

    public void setResult(Object o){
        result = String.valueOf(o);
    }

    public String getResult() {
        return result;
    }
}
