package com.billy.cc.core.component.remote;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;

import com.billy.cc.core.component.IParamJsonConverter;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * 将参数转换以进行跨进程传递
     * @param data 参数列表
     * @return 转换之后的参数列表
     */
    static HashMap<String, Object> toRemoteMap(Map<String, Object> data) {
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
    static HashMap<String, Object> toLocalMap(Map<String, Object> map) {
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
                || v instanceof Bundle                  || v instanceof PersistableBundle
                || v instanceof Parcelable              || v instanceof Parcelable[]
                || v instanceof CharSequence[]          || v instanceof IBinder
                || v instanceof Size                    || v instanceof SizeF ) {
            return v;
        } else if (v instanceof SparseArray) {
            SparseArray sp = new SparseArray();
            for(int i = 0; i < sp.size(); i++) {
                sp.put(sp.keyAt(i), convertParam(sp.valueAt(i)));
            }
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

    abstract static class BaseParam implements Parcelable {
        Class<?> clazz;
        int hashCode;

        BaseParam(Object obj) {
            clazz = obj.getClass();
            hashCode = obj.hashCode();
        }

        BaseParam(Parcel in) {
            hashCode = in.readInt();
            clazz = (Class<?>) in.readSerializable();
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
                e.printStackTrace();
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
                e.printStackTrace();
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

}
