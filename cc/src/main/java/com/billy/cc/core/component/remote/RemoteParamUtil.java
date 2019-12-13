package com.billy.cc.core.component.remote;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.SparseArray;

import com.billy.cc.core.component.CCUtil;
import com.billy.cc.core.component.IParamJsonConverter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.billy.cc.core.component.CCUtil.put;

/**
 * 用于跨进程的参数转换
 * @author billy.qi
 * @since 18/6/3 20:49
 */
@SuppressWarnings("unchecked")
public class RemoteParamUtil {

    static IParamJsonConverter paramJsonConverter;

    static void initRemoteCCParamJsonConverter(IParamJsonConverter converter) {
        paramJsonConverter = converter;
    }

    public static String convertObject2JsonString(Object object) {
        if (paramJsonConverter != null && object != null) {
            return paramJsonConverter.object2Json(object);
        }
        return object == null ? null : object.toString();
    }

    public static <T> T convertJson2Object(String json, Class<T> clazz) {
        if (!TextUtils.isEmpty(json) && clazz != null && paramJsonConverter != null) {
            return paramJsonConverter.json2Object(json, clazz);
        }
        return null;
    }

    /**
     * 将参数转换以进行跨进程传递
     * @param data 参数列表
     * @return 转换之后的参数列表
     */
    public static HashMap<String, Object> toRemoteMap(Map<String, Object> data) {
        HashMap<String, Object> map = null;
        if (data != null) {
            map = new HashMap<>(data.size());
            for (String key : data.keySet()) {
                map.put(key, convertParam(data.get(key)));
            }
        }
        return map;
    }

    /**
     * 将跨进程传递过来的参数转换为本地参数
     * @param map 跨进程传过来的参数列表
     * @return 本地可用的参数列表
     */
    public static HashMap<String, Object> toLocalMap(Map<String, Object> map) {
        HashMap<String, Object> data = null;
        if (map != null) {
            data = new HashMap<>(map.size());
            for (String key : map.keySet()) {
                data.put(key, restoreParam(map.get(key)));
            }
        }
        return data;
    }
    
    private static Object restoreParam(Object v) {
        if (v instanceof SparseArray) {
            SparseArray sp = (SparseArray) v;
            for(int i = 0; i < sp.size(); i++) {
                sp.put(sp.keyAt(i), restoreParam(sp.valueAt(i)));
            }
            return v;
        } else if (v instanceof BaseParam) {
            return ((BaseParam) v).restore();
        } else {
            return v;
        }
    }

    static Object convertParam(Object v) {
        if (v == null
                || v instanceof String                  || v instanceof Integer
                || v instanceof Long                    || v instanceof Float
                || v instanceof Double                  || v instanceof Boolean
                || v instanceof Short                   || v instanceof Byte
                || v instanceof CharSequence
                || v instanceof String[]                || v instanceof int[]
                || v instanceof byte[]                  || v instanceof long[]
                || v instanceof double[]                || v instanceof boolean[]
                || v instanceof Bundle
                || v instanceof Parcelable              || v instanceof Parcelable[]
                || v instanceof CharSequence[]          || v instanceof IBinder
                ) {
            return v;
        } else if (v instanceof SparseArray) {
            SparseArray sa = (SparseArray) v;
            SparseArray sp = new SparseArray();
            for(int i = 0; i < sa.size(); i++) {
                sp.put(sa.keyAt(i), convertParam(sa.valueAt(i)));
            }
            //parcel.writeMap方法支持内部key-value为SparseArray类型
            // 此处无需额外封装，返回SparseArray即可
            // 但需要对SparseArray内的item进行封装
            return sp;
        } else if (v instanceof Map) {
            return new MapParam(v);
        } else if (v instanceof  Collection) {
            return new CollectionParam(v);
        } else {
            Class<?> clazz = v.getClass();
            if (clazz.isArray()) {
                return new ArrayParam(v);
            } else if (v instanceof  Serializable) {
                return v;
            } else {
                return new ObjParam(v);
            }
        }
    }

    public abstract static class BaseParam implements Parcelable {
        Class<?> clazz;
        int hashCode;

        BaseParam(Object obj) {
            clazz = obj.getClass();
            hashCode = obj.hashCode();
        }

        BaseParam(Parcel in) {
            hashCode = in.readInt();
            try {
                clazz = (Class<?>) in.readSerializable();
            } catch(Exception e) {
                CCUtil.printStackTrace(e);
            }
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @NonNull
        protected JSONObject toJson() {
            JSONObject json = new JSONObject();
            put(json, "class", clazz);
            return json;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(hashCode);
            dest.writeSerializable(clazz);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * 转换回对象
         * @return 还原的对象
         */
        public abstract Object restore();

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    static class ObjParam extends BaseParam {
        String json;

        ObjParam(Object obj) {
            super(obj);
            if (paramJsonConverter != null) {
                json = paramJsonConverter.object2Json(obj);
            }
        }

        @NonNull
        @Override
        protected JSONObject toJson() {
            JSONObject jsonObject = super.toJson();
            put(jsonObject, "value", json);
            return jsonObject;
        }

        @Override
        public Object restore() {
            if (paramJsonConverter != null) {
                return paramJsonConverter.json2Object(json, clazz);
            }
            return null;
        }

        ObjParam(Parcel in) {
            super(in);
            json = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(json);
        }

        public static final Creator<ObjParam> CREATOR = new Creator<ObjParam>() {
            @Override
            public ObjParam createFromParcel(Parcel in) {
                return new ObjParam(in);
            }

            @Override
            public ObjParam[] newArray(int size) {
                return new ObjParam[size];
            }
        };
    }

    /**
     * 解析数组
     * 为了避免父类数组中的子类对象解析不完整，不直接使用paramJsonConverter
     */
    static class ArrayParam extends BaseParam {
        ArrayList params = new ArrayList();
        int length;

        ArrayParam(Object obj) {
            super(obj);
            length = Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                params.add(convertParam(Array.get(obj, i)));
            }
        }

        @NonNull
        @Override
        protected JSONObject toJson() {
            JSONObject jsonObject = super.toJson();
            put(jsonObject, "length", length);
            JSONArray array = new JSONArray();
            for (int i = 0; i < params.size(); i++) {
                array.put(params.get(i));
            }
            put(jsonObject, "value", array);
            return jsonObject;
        }

        @Override
        public Object restore() {
            Object obj = Array.newInstance(clazz.getComponentType(), length);
            int size = params.size();
            for (int i = 0; i < size; i++) {
                Array.set(obj, i, restoreParam(params.get(i)));
            }
            return obj;
        }

        ArrayParam(Parcel in) {
            super(in);
            length = in.readInt();
            params = in.readArrayList(getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(length);
            dest.writeList(params);
        }

        public static final Creator<ArrayParam> CREATOR = new Creator<ArrayParam>() {
            @Override
            public ArrayParam createFromParcel(Parcel in) {
                return new ArrayParam(in);
            }

            @Override
            public ArrayParam[] newArray(int size) {
                return new ArrayParam[size];
            }
        };
    }

    /**
     * 由于泛型擦除，不直接使用paramJsonConverter
     */
    static class CollectionParam extends BaseParam {
        ArrayList<Object> params = new ArrayList<>();

        CollectionParam(Object obj) {
            super(obj);
            for (Object o : ((Collection) obj)) {
                params.add(convertParam(o));
            }
        }

        @NonNull
        @Override
        protected JSONObject toJson() {
            JSONObject jsonObject = super.toJson();
            put(jsonObject, "length", params.size());
            JSONArray array = new JSONArray();
            for (int i = 0; i < params.size(); i++) {
                array.put(params.get(i));
            }
            put(jsonObject, "value", array);
            return jsonObject;
        }

        @Override
        public Object restore() {
            try {
                Object o = clazz.getConstructor().newInstance();
                Collection collection = (Collection) o;
                for (Object param : params) {
                    collection.add(restoreParam(param));
                }
                return o;
            } catch(Exception e) {
                CCUtil.printStackTrace(e);
            }
            return null;
        }
        CollectionParam(Parcel in) {
            super(in);
            params = in.readArrayList(getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeList(params);
        }

        public static final Creator<CollectionParam> CREATOR = new Creator<CollectionParam>() {
            @Override
            public CollectionParam createFromParcel(Parcel in) {
                return new CollectionParam(in);
            }

            @Override
            public CollectionParam[] newArray(int size) {
                return new CollectionParam[size];
            }
        };
    }

    static class MapParam extends BaseParam {
        HashMap<Object, Object> params = new HashMap<>();

        MapParam(Object obj) {
            super(obj);
            Map map = (Map) obj;
            for (Object o : map.keySet()) {
                params.put(convertParam(o), convertParam(map.get(o)));
            }
        }

        @NonNull
        @Override
        protected JSONObject toJson() {
            JSONObject jsonObject = super.toJson();
            JSONObject value = new JSONObject();
            for (Map.Entry<Object, Object> entry : params.entrySet()) {
                Object key = entry.getKey();
                if (key == null) {
                    key = JSONObject.NULL;
                }
                put(value, key.toString(), entry.getValue());
            }
            put(jsonObject, "value", value);
            return jsonObject;
        }

        @Override
        public Object restore() {
            try {
                Object o = clazz.getConstructor().newInstance();
                Map map = (Map) o;
                for (Object param : params.keySet()) {
                    Object key = restoreParam(param);
                    Object value = restoreParam(params.get(param));
                    map.put(key, value);
                }
                return o;
            } catch(Exception e) {
                CCUtil.printStackTrace(e);
            }
            return null;
        }
        
        MapParam(Parcel in) {
            super(in);
            params = in.readHashMap(getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeMap(params);
        }

        public static final Creator<MapParam> CREATOR = new Creator<MapParam>() {
            @Override
            public MapParam createFromParcel(Parcel in) {
                return new MapParam(in);
            }

            @Override
            public MapParam[] newArray(int size) {
                return new MapParam[size];
            }
        };
    }

    static {
        // 通过插件自动注册自定义的跨进程json转换器
        // initRemoteCCParamJsonConverter(new GsonParamConverter());
    }
}
