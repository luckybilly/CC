package com.billy.cc.core.component;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import com.billy.cc.core.component.remote.RemoteParamUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author billy.qi
 * @since 17/7/9 18:37
 */
public class CCUtil {
    public static final String PROCESS_UNKNOWN = "UNKNOWN";

    public static Map<String, Object> convertToMap(JSONObject json) {
        Map<String, Object> params = null;
        try{
            if (json != null) {
                params = new HashMap<>(json.length());
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        Object value = json.get(key);
                        if (value == JSONObject.NULL) {
                            value = null;
                        }
                        params.put(key, value);
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

    @SuppressLint("UseSparseArrays")
    private static Object convertObjectToJson(Object v) {
        String jsonString;
        if (v == null) {
            return JSONObject.NULL;
        } else if (v instanceof RemoteParamUtil.BaseParam) {
            RemoteParamUtil.BaseParam param = (RemoteParamUtil.BaseParam) v;
            jsonString = param.toString();
        } else if (v instanceof SparseArray) {
            Map<Integer, Object> map = new HashMap<>();
            SparseArray sp = (SparseArray) v;
            for(int i = 0; i < sp.size(); i++) {
                map.put(sp.keyAt(i), sp.valueAt(i));
            }
            return convertToJson(map);
        } else if (v instanceof SparseIntArray) {
            Map<Integer, Integer> map = new HashMap<>();
            SparseIntArray sp = (SparseIntArray) v;
            for(int i = 0; i < sp.size(); i++) {
                map.put(sp.keyAt(i), sp.valueAt(i));
            }
            return convertToJson(map);
        } else if (v instanceof SparseBooleanArray) {
            Map<Integer, Boolean> map = new HashMap<>();
            SparseBooleanArray sp = (SparseBooleanArray) v;
            for(int i = 0; i < sp.size(); i++) {
                map.put(sp.keyAt(i), sp.valueAt(i));
            }
            return convertToJson(map);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && v instanceof SparseLongArray) {
            Map<Integer, Long> map = new HashMap<>();
            SparseLongArray sp = (SparseLongArray) v;
            for(int i = 0; i < sp.size(); i++) {
                map.put(sp.keyAt(i), sp.valueAt(i));
            }
            return convertToJson(map);

        } else if ( v instanceof String                  || v instanceof Integer
                || v instanceof Long                    || v instanceof Float
                || v instanceof Double                  || v instanceof Boolean
                || v instanceof Short                   || v instanceof Byte
                || v instanceof CharSequence            || v instanceof Character
                ) {
            return v;
        } else {
            jsonString = RemoteParamUtil.convertObject2JsonString(v);
        }
        if (jsonString == null) {
            return null;
        }
        jsonString = jsonString.trim();
        try {
            if (jsonString.startsWith("[")) {
                return new JSONArray(jsonString);
            } else if (jsonString.startsWith("{")) {
                return new JSONObject(jsonString);
            } else {
                return jsonString;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static JSONObject convertToJson(Map<?, ?> map) {
        if (map != null) {
            try{
                JSONObject json = new JSONObject();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object value = entry.getValue();
                    Object key = entry.getKey();
                    if (key == null) {
                        key = "null";
                    }
                    if (value == null) {
                        json.put(key.toString(), null);
                    } else {
                        json.put(key.toString(), convertObjectToJson(value));
                    }
                }
                return json;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Boolean isRunningMainProcess = null;
    private static String curProcessName = null;
    /**
     * 进程是否以包名在运行（当前进程是否为主进程）
     */
    static boolean isMainProcess(){
        if (isRunningMainProcess == null) {
            Application application = CC.getApplication();
            if (application == null) {
                return false;
            }
            isRunningMainProcess = application.getPackageName().equals(getCurProcessName());
        }
        return isRunningMainProcess;
    }

    /**
     * 获取当前进程的名称
     * @return 进程名
     */
    public static String getCurProcessName() {
        if (curProcessName != null) {
            return curProcessName;
        }
        Application application = CC.getApplication();
        if (application == null) {
            return PROCESS_UNKNOWN;
        }
        try {
            ActivityManager manager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                List<RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
                for (RunningAppProcessInfo appProcess : processes) {
                    if (appProcess.pid == android.os.Process.myPid()) {
                        curProcessName = appProcess.processName;
                        return curProcessName;
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return PROCESS_UNKNOWN;
    }

    public static String[] getCurProcessPkgList() {
        Application application = CC.getApplication();
        if (application == null) {
            return null;
        }
        try {
            ActivityManager manager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                List<RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
                for (RunningAppProcessInfo appProcess : processes) {
                    if (appProcess.pid == android.os.Process.myPid()) {
                        return appProcess.pkgList;
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 反射获取application对象
     * @return application
     */
    static Application initApplication() {
        try {
            //兼容android P，直接调用@hide注解的方法来获取application对象
            Application app = ActivityThread.currentApplication();
            if (app != null) {
                return app;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        try {
            //兼容android P，直接调用@hide注解的方法来获取application对象
            Application app = AppGlobals.getInitialApplication();
            if (app != null) {
                return app;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void put(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
