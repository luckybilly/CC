package com.billy.cc.core.component;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author billy.qi
 * @since 17/7/9 18:37
 */
class CCUtil {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    static void invokeCallback(CC cc, CCResult result) {
        if (cc == null) {
            return;
        }
        CC.CC_MAP.remove(cc.getCallId());
        if (cc.getCallback() == null) {
            return;
        }
        if (result == null) {
            result = CCResult.defaultNullResult();
        }
        if (cc.isCallbackOnMainThread()) {
            HANDLER.post(new CallbackRunnable(cc, result));
        } else {
            try{
                cc.getCallback().onResult(cc, result);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static class CallbackRunnable implements Runnable {
        private final CC cc;
        IComponentCallback callback;
        CCResult result;

        CallbackRunnable(CC cc, CCResult result) {
            this.cc = cc;
            this.callback = cc.getCallback();
            this.result = result;
        }

        @Override
        public void run() {
            try{
                callback.onResult(cc, result);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

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
