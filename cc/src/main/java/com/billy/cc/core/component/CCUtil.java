package com.billy.cc.core.component;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
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
@SuppressLint("PrivateApi")
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

    static JSONObject convertToJson(Map<String, Object> map) {
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
    /**
     * 进程是否以包名在运行（当前进程是否为主进程）
     */
    static boolean isMainProcess(){
        if (isRunningMainProcess == null) {
            Application application = CC.getApplication();
            if (application == null) {
                return false;
            }
            boolean main = false;
            try {
                ActivityManager manager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
                if (manager == null) {
                    return true;
                }
                List<RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
                for (RunningAppProcessInfo appProcess : processes) {
                    if (appProcess.pid == android.os.Process.myPid()
                            && application.getPackageName().equals(appProcess.processName)) {
                        main = true;
                        break;
                    }
                }
            }catch (Exception ex){
                ex.printStackTrace();

            }
            isRunningMainProcess = main;
        }
        return isRunningMainProcess;
    }

    /**
     * 反射获取application对象
     * @return application
     */
    static Application initApplication() {
        try {
            //通过反射的方式来获取当前进程的application对象
            Application app = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (app != null) {
                return app;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        try {
            Application app = (Application) Class.forName("android.app.AppGlobals")
                    .getMethod("getInitialApplication").invoke(null);
            if (app != null) {
                return app;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    static Bundle convertToBundle(Map<String, Object> map) {
//        Bundle bundle = new Bundle();
//        if (map != null && !map.isEmpty()) {
//            final IParamJsonConverter converter = paramJsonConverter;
//            JSONObject convertedBeans = new JSONObject();
//            Set<Map.Entry<String, Object>> entries = map.entrySet();
//            String key;
//            Object value;
//            for (Map.Entry<String, Object> entry : entries) {
//                key = entry.getKey();
//                value = entry.getValue();
//                if (key == null || value == null) {
//                    continue;
//                }
//                boolean success = putIntoBundle(bundle, key, value);
//                if (!success && converter != null) {
//                    bundle.putString(key, converter.object2Json(value));
//                    try {
//                        convertedBeans.put(key, value.getClass().getName());
//                    } catch(Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            bundle.putString(RemoteCCInterceptor.KEY_PARAM_TYPES, convertedBeans.toString());
//        }
//        return bundle;
//    }
//
//    private static boolean putIntoBundle(Bundle bundle, String key, Object value) {
//        if (value instanceof String) {
//            bundle.putString(key, (String)value);
//        } else if (value instanceof String[]) {
//            bundle.putStringArray(key, (String[])value);
//        } else if (value instanceof Integer) {
//            bundle.putInt(key, (Integer)value);
//        } else if (value instanceof int[]) {
//            bundle.putIntArray(key, (int[]) value);
//        } else if (value instanceof Long) {
//            bundle.putLong(key, (Long)value);
//        } else if (value instanceof long[]) {
//            bundle.putLongArray(key, (long[]) value);
//        } else if (value instanceof Character) {
//            bundle.putChar(key, (Character)value);
//        } else if (value instanceof char[]) {
//            bundle.putCharArray(key, (char[]) value);
//        } else if (value instanceof Short) {
//            bundle.putShort(key, (Short)value);
//        } else if (value instanceof short[]) {
//            bundle.putShortArray(key, (short[]) value);
//        } else if (value instanceof Byte) {
//            bundle.putByte(key, (Byte)value);
//        } else if (value instanceof byte[]) {
//            bundle.putByteArray(key, (byte[]) value);
//        } else if (value instanceof Float) {
//            bundle.putFloat(key, (Float)value);
//        } else if (value instanceof float[]) {
//            bundle.putFloatArray(key, (float[]) value);
//        } else if (value instanceof Double) {
//            bundle.putDouble(key, (Double)value);
//        } else if (value instanceof double[]) {
//            bundle.putDoubleArray(key, (double[]) value);
//        } else if (value instanceof Boolean) {
//            bundle.putBoolean(key, (Boolean) value);
//        } else if (value instanceof boolean[]) {
//            bundle.putBooleanArray(key, (boolean[]) value);
//        } else if (value instanceof Parcelable) {
//            bundle.putParcelable(key, (Parcelable)value);
//        } else if (value instanceof Parcelable[]) {
//            bundle.putParcelableArray(key, (Parcelable[])value);
//        } else if (value instanceof Serializable) {
//            bundle.putSerializable(key, (Serializable)value);
//        } else if (value instanceof Binder) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                bundle.putBinder(key, (Binder)value);
//            }
//        } else {
//            return false;
//        }
//        return true;
//    }
//
//    static Map<String, Object> convertToMap(Bundle bundle) {
//        Map<String, Object> map = new HashMap<>();
//        if (bundle != null && !bundle.isEmpty()) {
//            String str = getAndRemoveStringFromBundle(bundle, RemoteCCInterceptor.KEY_PARAM_TYPES);
//            JSONObject json;
//            try {
//                json = new JSONObject(str);
//            } catch(Exception e) {
//                e.printStackTrace();
//                json = new JSONObject();
//            }
//            final IParamJsonConverter converter = paramJsonConverter;
//            Set<String> keys = bundle.keySet();
//            for (String key : keys) {
//                Object value = bundle.get(key);
//                if (converter != null && value instanceof String) {
//                    String className = json.optString(key);
//                    if (!TextUtils.isEmpty(className)) {
//                        Class clazz = getParamClassByName(className);
//                        if (clazz != null) {
//                            Object result = converter.json2Object((String) value, clazz);
//                            map.put(key, result);
//                            continue;
//                        }
//                    }
//                }
//                map.put(key, value);
//            }
//        }
//        return map;
//    }
//
//    static String getAndRemoveStringFromIntent(Intent intent, String bundleKey) {
//        return getAndRemoveStringFromBundle(intent.getExtras(), bundleKey);
//    }
//
//    private static String getAndRemoveStringFromBundle(Bundle bundle, String bundleKey) {
//        String str = bundle.getString(bundleKey);
//        bundle.remove(bundleKey);
//        return str;
//    }
//
//    private static Map<String, Class> CLASS_CACHE;
//    @Nullable
//    private static Class getParamClassByName(String className) {
//        if (CLASS_CACHE == null) {
//            CLASS_CACHE = new HashMap<>(16);
//        }
//        Class clazz = CLASS_CACHE.get(className);
//        if (clazz == null) {
//            try {
//                clazz = Class.forName(className);
//                CLASS_CACHE.put(className, clazz);
//            } catch (ClassNotFoundException e) {
//                //当前组件中没有对应的类，打印日志提示开发者
//                CC.logError("get parameter class failed:" + e.getMessage());
//            }
//        }
//        return clazz;
//    }
//
//    static HashMap<String, String> prepareForRemoteCCResult(HashMap<String, Object> data) {
//        HashMap<String, String> map = new HashMap<>();
//        if (data != null) {
//            Iterator<Map.Entry<String, Object>> iterator = data.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<String, Object> entry = iterator.next();
//                Object value = entry.getValue();
//                if (value != null && shouldConvert(value)) {
//                    String key = entry.getKey();
//                    if (paramJsonConverter != null) {
//                        String str = paramJsonConverter.object2Json(value);
//                        data.put(key, str);
//                        map.put(key, value.getClass().getName());
//                    } else {
//                        iterator.remove();
//                        CC.logError("remote ccResult convert failed,"
//                                + " no IParamJsonConverter for:" + value);
//                    }
//                }
//            }
//        }
//        return map;
//    }
//    private static final Class<?>[] CONVERT_NEEDLESS_CLASSES = new Class[]{
//            Byte.class, Boolean.class, Short.class, Character.class, Integer.class, Long.class, Float.class, Double.class, String.class
//            , byte[].class, boolean[].class, short[].class, char[].class, int[].class, long[].class, float[].class, double[].class, String[].class
//            , Byte[].class, Boolean[].class, Short[].class, Character[].class, Integer[].class, Long[].class, Float[].class, Double[].class
//    };
//
//    private static boolean shouldConvert(Object value) {
//        if(value instanceof Serializable) {
//            Class<?> clazz = value.getClass();
//            for (Class<?> c : CONVERT_NEEDLESS_CLASSES) {
//                if (c == clazz) {
//                    //基础数据类型和基础数据类型的数组不需要json转换
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
//
//    static void restoreCCResult(HashMap<String, Object> data, HashMap<String, String> types) {
//        if (data == null || types == null) {
//            return;
//        }
//        if (paramJsonConverter == null) {
//            if (!types.isEmpty() && CC.DEBUG) {
//                for (String value : types.values()) {
//                    CC.logError("remote ccResult restore failed, "
//                            + "no IParamJsonConverter for class:" + value);
//                }
//            }
//            return;
//        }
//        for (Map.Entry<String, String> entry : types.entrySet()) {
//            String key = entry.getKey();
//            Object obj = data.get(key);
//            if (obj instanceof String) {
//                String json = (String) obj;
//                String className = entry.getValue();
//                if (!TextUtils.isEmpty(className)) {
//                    Class clazz = getParamClassByName(className);
//                    if (clazz != null) {
//                        obj = paramJsonConverter.json2Object(json, clazz);
//                        data.put(key, obj);
//                    }
//                }
//            }
//        }
//        types.clear();
//    }
}
