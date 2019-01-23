package com.billy.cc.core.component;

/**
 * app间传递参数时，用于将自定义类型转换为json进行传递，并在传递成功后复原为自定义类型
 * @author billy.qi
 * @since 18/5/28 16:11
 */
public interface IParamJsonConverter {

    /**
     * 将json字符串转换为对象
     * @param json json字符串
     * @param clazz 类型
     * @param <T> 泛型
     * @return json转换的对象
     */
    <T> T json2Object(String json, Class<T> clazz);

    /**
     * Object to json
     *
     * @param instance 要跨app传递的对象
     * @return json字符串
     */
    String object2Json(Object instance);
}
