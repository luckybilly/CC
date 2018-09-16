package com.billy.cc.core.component;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.Application;
import android.content.Context;

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

    public static JSONObject convertToJson(Map<String, Object> map) {
        if (map != null) {
            try{
                JSONObject json = new JSONObject();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Object value = entry.getValue();
                    if (value == null) {
                        json.put(entry.getKey(), null);
                    } else {
                        json.put(entry.getKey(), value.toString());
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
