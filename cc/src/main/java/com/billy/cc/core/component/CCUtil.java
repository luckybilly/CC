package com.billy.cc.core.component;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import com.billy.cc.core.component.remote.RemoteCC;
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

    /**
     * 将一个json对象解析为map对象
     * @param json json对象
     * @return map对象
     */
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
                        CCUtil.printStackTrace(e);
                    }
                }
            }
        } catch(Exception e) {
            CCUtil.printStackTrace(e);
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
        } else if (v instanceof Map) {
            return convertToJson((Map<?, ?>) v);
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
            try {
                jsonString = RemoteParamUtil.convertObject2JsonString(v);
            } catch(Exception e) {
                CCUtil.printStackTrace(e);
                jsonString = null;
            }
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
            CCUtil.printStackTrace(e);
            return null;
        }
    }

    /**
     * 将一个map对象转换成json对象
     * 1. 用于日志输出
     * 2. 用于需要将CC或CCResult转成json进行传递的场景（如：JsBridge传值）
     * @param map map对象
     * @return json对象
     */
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
                CCUtil.printStackTrace(e);
            }
        }
        return null;
    }

    private static Boolean isRunningMainProcess = null;
    private static String curProcessName = null;
    /**
     * 进程是否以包名在运行（当前进程是否为主进程）
     */
    public static boolean isMainProcess(){
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
            CCUtil.printStackTrace(e);
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
            CCUtil.printStackTrace(e);
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
            CCUtil.printStackTrace(e);
        }
        try {
            //兼容android P，直接调用@hide注解的方法来获取application对象
            Application app = AppGlobals.getInitialApplication();
            if (app != null) {
                return app;
            }
        } catch (Exception e) {
            CCUtil.printStackTrace(e);
        }
        return null;
    }

    public static void put(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch(Exception e) {
            CCUtil.printStackTrace(e);
        }
    }


    public static final String EXTRA_KEY_CALL_ID = "cc_extra_call_id";
    public static final String EXTRA_KEY_REMOTE_CC = "cc_extra_remote_cc";

    /**
     * 跳转到指定的activity，
     * 并将CC的参数通过extra传递给该activity，
     * 可通过以下方式获取指定参数：<br>
     *  1. {@link CCUtil#getNavigateParam(Bundle, String, Object)}<br>
     *  2. {@link CCUtil#getNavigateParam(Activity, String, Object)}<br>
     * 可通过 {@link #getNavigateCallId(Activity)} 获取调起该页面的CC.callId
     * @param cc 当前CC调用
     * @param activityClass 要跳转到的activity
     */
    public static void navigateTo(CC cc, Class<? extends Activity> activityClass) {
        Intent intent = createNavigateIntent(cc, activityClass);
        RemoteCC remoteCC = new RemoteCC(cc);
        intent.putExtra(EXTRA_KEY_REMOTE_CC, remoteCC);
        intent.putExtra(EXTRA_KEY_CALL_ID, cc.getCallId());
        cc.getContext().startActivity(intent);
    }

    /**
     * 为activity跳转创建一个intent，如果调用方没设置context
     * 使用application来startActivity时自动添加{@link Intent#FLAG_ACTIVITY_NEW_TASK}
     * @param cc CC对象
     * @param activityClass 要跳转的activity
     * @return intent
     */
    public static Intent createNavigateIntent(CC cc, Class<? extends Activity> activityClass) {
        Context context = cc.getContext();
        Intent intent = new Intent(context, activityClass);
        if (!(context instanceof Activity)) {
            //调用方没有设置context或app间组件跳转，context为application
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    /**
     * 在组件类中通过{@link #navigateTo(CC, Class)}跳转页面后，可通过此方法快捷获取CC的参数
     * @param activity activity
     * @param key key
     * @param defaultValue 默认值
     * @param <T> 返回值的类型，可根据接收值及defaultValue的类型来自动确定
     * @return CC中携带的参数（若参数列表中不包含该参数，则返回默认值defaultValue）
     */
    public static <T> T getNavigateParam(@NonNull Activity activity, String key, T defaultValue) {
        Intent intent = activity.getIntent();
        if (intent != null) {
            return getNavigateParam(intent.getExtras(), key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * 在组件类中通过{@link #navigateTo(CC, Class)}跳转页面后，可通过此方法快捷获取CC的callId
     */
    public static String getNavigateCallId(@NonNull Activity activity) {
        Intent intent = activity.getIntent();
        if (intent != null) {
            return intent.getStringExtra(EXTRA_KEY_CALL_ID);
        }
        return null;
    }
    /**
     * 在组件类中通过{@link #navigateTo(CC, Class)}跳转页面后，可通过此方法快捷获取CC的参数
     * @param bundle bundle参数
     * @param key 需要取值的key
     * @param defaultValue 默认值
     * @param <T> 返回值的类型，可根据接收值及defaultValue的类型来自动确定
     * @return CC中携带的参数（若参数列表中不包含该参数，则返回默认值defaultValue）
     */
    public static <T> T getNavigateParam(Bundle bundle, String key, T defaultValue) {
        if (bundle != null) {
            RemoteCC remoteCc = bundle.getParcelable(EXTRA_KEY_REMOTE_CC);
            if (remoteCc != null) {
                return getParamItem(remoteCc, key, defaultValue);
            }
        }
        return defaultValue;
    }

    private static <T> T getParamItem(RemoteCC remoteCc, String key, T defaultValue) {
        Map<String, Object> params = remoteCc.getParams();
        if (!params.containsKey(key)) {
            return defaultValue;
        }
        Object o = params.get(key);
        try {
            return (T) o;
        } catch (ClassCastException e) {
            if (CC.DEBUG) {
                CCUtil.printStackTrace(e);
                CC.logError("get cc param from bundle failed: class cast failed! key=%s, returns defaultValue:" + defaultValue, key);
            }
            return defaultValue;
        }
    }

    public static void printStackTrace(Throwable t) {
        if (CC.DEBUG && t != null) {
            t.printStackTrace();
        }
    }
}
