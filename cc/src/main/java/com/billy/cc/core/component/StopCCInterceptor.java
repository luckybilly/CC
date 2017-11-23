package com.billy.cc.core.component;

/**
 * 中止CC调用，直接回调错误结果
 * @author billy.qi
 */
class StopCCInterceptor implements ICCInterceptor {

    private int errorCode;

    StopCCInterceptor(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public CCResult intercept(Chain chain) {
        return CCResult.error(errorCode);
    }
}
