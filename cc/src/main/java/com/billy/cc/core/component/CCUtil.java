package com.billy.cc.core.component;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author billy.qi
 * @since 17/7/9 18:37
 */
class CCUtil {

    static Map<String, Object> convertToMap(JSONObject json) {
        Map<String, Object> params = null;
        try{
            if (json != null) {
                params = new HashMap<>(json.length());
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        params.put(key, json.get(key));
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return params;
    }

    static JSONObject convertToJson(Map<String, Object> map) {
        if (map != null) {
            try{
                return new JSONObject(map);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
