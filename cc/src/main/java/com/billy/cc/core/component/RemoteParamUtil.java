package com.billy.cc.core.component;

import android.util.SparseArray;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * @author billy.qi
 * @since 18/6/3 20:49
 */
class RemoteParamUtil {

    private static IParamJsonConverter paramJsonConverter;

    static void initRemoteCCParamJsonConverter(IParamJsonConverter converter) {
        paramJsonConverter = converter;
    }

    /**
     * 将参数转换以进行跨进程传递
     * @param data 参数列表
     * @return 转换之后的参数列表
     */
    static HashMap<String, BaseParam> toRemoteMap(Map<String, Object> data) {
        HashMap<String, BaseParam> map = null;
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
    static Map<String, Object> toLocalMap(HashMap<String, BaseParam> map) {
        Map<String, Object> data = null;
        if (map != null) {
            data = new HashMap<>(map.size());
            for (String key : map.keySet()) {
                data.put(key, map.get(key).restore());
            }
        }
        return data;
    }

    static BaseParam convertParam(Object object) {
        if (object == null) {
            return new NullParam();
        } else if (keepSerializable(object)) {
            return new PrimitiveParam(object);
        }
        Class<?> clazz = object.getClass();
        if (clazz.isArray()) {
            return new ArrayParam(object);
        } else if (object instanceof Collection) {
            return new CollectionParam(object);
        } else if (object instanceof SparseArray) {
            return new SparseArrayParam(object);
        } else if (object instanceof Map) {
            return new MapParam(object);
        } else {
            return new SingleParam(object);
        }
    }

    abstract static class BaseParam implements Serializable {
        private static final long serialVersionUID = 1L;
        Class<?> clazz;
        int hashCode;

        BaseParam(Object obj) {
            if (obj != null) {
                clazz = obj.getClass();
                hashCode = clazz.hashCode();
            }
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

    static class NullParam extends BaseParam {
        NullParam() {
            super(null);
        }

        @Override
        public Object restore() {
            return null;
        }
    }
    static class PrimitiveParam extends BaseParam {
        Object value;
        PrimitiveParam(Object obj) {
            super(null);
            value = obj;
        }

        @Override
        public Object restore() {
            return value;
        }
    }

    static class SingleParam extends BaseParam {
        String json;

        SingleParam(Object obj) {
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
    }

    /**
     * 解析数组
     * 为了避免父类数组中的子类对象解析不完整，不直接使用paramJsonConverter
     */
    static class ArrayParam extends BaseParam {
        LinkedList<BaseParam> params = new LinkedList<>();
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
                Array.set(obj, i, params.get(i).restore());
            }
            return obj;
        }
    }

    /**
     * 由于泛型擦除，不直接使用paramJsonConverter
     */
    static class CollectionParam extends BaseParam {
        LinkedList<BaseParam> params = new LinkedList<>();

        CollectionParam(Object obj) {
            super(obj);
            for (Object next : ((Collection) obj)) {
                params.add(convertParam(next));
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object restore() {
            try {
                Object o = clazz.getConstructor().newInstance();
                Collection collection = (Collection) o;
                for (BaseParam param : params) {
                    collection.add(param.restore());
                }
                return o;
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static class MapParam extends BaseParam {
        HashMap<BaseParam, BaseParam> params = new HashMap<>();

        MapParam(Object obj) {
            super(obj);
            Map map = (Map) obj;
            for (Object o : map.keySet()) {
                BaseParam key = convertParam(o);
                BaseParam value = convertParam(map.get(o));
                params.put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object restore() {
            try {
                Object o = clazz.getConstructor().newInstance();
                Map map = (Map) o;
                for (BaseParam param : params.keySet()) {
                    Object key = param.restore();
                    Object value = params.get(param).restore();
                    map.put(key, value);
                }
                return o;
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    static class SparseArrayParam extends BaseParam {
        HashMap<Integer, BaseParam> params = new HashMap<>();

        SparseArrayParam(Object obj) {
            super(obj);
            SparseArray sparseArray = (SparseArray) obj;
            for (int i = 0; i < sparseArray.size(); i++) {
                int key = sparseArray.keyAt(i);
                BaseParam value = convertParam(sparseArray.get(key));
                params.put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object restore() {
            try {
                SparseArray sparseArray = new SparseArray(params.size());
                for (Integer key : params.keySet()) {
                    Object value = params.get(key).restore();
                    sparseArray.put(key, value);
                }
                return sparseArray;
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static Set<Class<? extends Serializable>> CONVERT_NEEDLESS_CLASSES = new HashSet<>(Arrays.asList(
        Byte.class, Boolean.class, Short.class, Character.class, Integer.class, Long.class, Float.class, Double.class, String.class
        , byte[].class, boolean[].class, short[].class, char[].class, int[].class, long[].class, float[].class, double[].class, String[].class
        , byte[][].class, boolean[][].class, short[][].class, char[][].class, int[][].class, long[][].class, float[][].class, double[][].class, String[][].class
        , byte[][][].class, boolean[][][].class, short[][][].class, char[][][].class, int[][][].class, long[][][].class, float[][][].class, double[][][].class, String[][][].class
        , Byte[].class, Boolean[].class, Short[].class, Character[].class, Integer[].class, Long[].class, Float[].class, Double[].class
        , Byte[][].class, Boolean[][].class, Short[][].class, Character[][].class, Integer[][].class, Long[][].class, Float[][].class, Double[][].class
        , Byte[][][].class, Boolean[][][].class, Short[][][].class, Character[][][].class, Integer[][][].class, Long[][][].class, Float[][][].class, Double[][][].class
    ));

    private static boolean keepSerializable(Object value) {
        if(value instanceof Serializable) {
            Class<?> clazz = value.getClass();
            if (CONVERT_NEEDLESS_CLASSES.contains(clazz)) {
                //基础数据类型和基础数据类型的数组不需要json转换
                return true;
            }
        }
        return false;
    }

}
