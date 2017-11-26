package com.billy.cc.demo;

import android.util.Log;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.Chain;
import com.billy.cc.core.component.ICCInterceptor;

import java.util.Map;

/**
 * 将返回结果中的字符串"billy"替换为"billy miss you"
 * @author billy.qi
 */
public class MissYouInterceptor implements ICCInterceptor {

    /**
     * 拦截器的拦截方法
     * 调用chain.proceed()方法让调用链继续向下执行
     * 在调用chain.proceed()方法之前，可以修改cc的参数
     * 在调用chain.proceed()方法之后，可以修改返回结果
     * @param chain 拦截器调用链
     * @return 调用结果对象
     */
    @Override
    public CCResult intercept(Chain chain) {
        //获取调用链处理的cc对象
        CC cc = chain.getCC();
        Map<String, Object> params = cc.getParams();
        //可以在拦截器中修改params，此处只是仅仅用来打印一下
        Log.i("MissYouInterceptor", "callId=" + cc.getCallId() + ", params=" + params);
        //传递拦截器调用链
        // 不调用chain.proceed()方法, 可以中止调用链的继续传递（中止组件调用）
        // 譬如：埋点组件调用网络请求组件发送埋点信息
        //      1. 可以添加一个本地缓存拦截器：无网络时直接缓存本地数据库，不调用埋点组件；
        //      2. 埋点返回结果 ccResult.isSuccess()为false，也缓存到本地数据库
        CCResult result = chain.proceed();
        //对返回的结果进行修改
        if (result.isSuccess()) {
            Map<String, Object> data = result.getDataMap();
            if (data != null) {
                for (String key : data.keySet()) {
                    Object value = data.get(key);
                    //将字符串中的"billy"替换为"billy miss you"
                    if (value != null && value instanceof String) {
                        String str = (String) value;
                        str = str.replaceAll("billy", "billy miss you");
                        data.put(key, str);
                    }
                }
            }
        }
        return result;
    }
}
