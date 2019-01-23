package com.billy.cc.demo.interceptors;

import android.util.Log;

import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.Chain;
import com.billy.cc.core.component.IGlobalCCInterceptor;

/**
 * 示例全局拦截器：日志打印
 * @author billy.qi
 * @since 18/5/26 11:42
 */
public class LogInterceptor implements IGlobalCCInterceptor {
    private static final String TAG = "LogInterceptor";

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public CCResult intercept(Chain chain) {
        Log.i(TAG, "============log before:" + chain.getCC());
        CCResult result = chain.proceed();
        Log.i(TAG, "============log after:" + result);
        return result;
    }
}
