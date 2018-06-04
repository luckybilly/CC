package com.billy.cc.demo.base;

import com.billy.cc.core.component.IParamJsonConverter;
import com.google.gson.Gson;

/**
 * 用Gson来进行跨app调用时的json转换
 * @author billy.qi
 * @since 18/5/28 19:48
 */
public class GsonParamConverter implements IParamJsonConverter {

    @Override
    public <T> T json2Object(String input, Class<T> clazz) {
        return new Gson().fromJson(input, clazz);
    }

    @Override
    public String object2Json(Object instance) {
        return new Gson().toJson(instance);
    }
}
