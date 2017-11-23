package com.billy.cc.core.component;

import java.util.concurrent.Callable;

/**
 * 启动拦截器调用链
 * @author billy.qi
 */
class CCProcessor implements Callable<CCResult> {

    private final Chain chain;

    CCProcessor(Chain chain) {
        this.chain = chain;
    }

    @Override
    public CCResult call() throws Exception {
        CC cc = chain.getCC();
        String callId = cc.getCallId();
        if (CC.VERBOSE_LOG) {
            CC.verboseLog(callId, "process cc at thread:" + Thread.currentThread().getName());
        }
        if (cc.isCanceled()) {
            return CCResult.error(CCResult.CODE_ERROR_CANCELED);
        }
        CCResult result = null;
        try {
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "start interceptor chain");
            }
            result = chain.proceed();
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "end interceptor chain.CCResult:" + result);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        //返回的结果，永不为null，默认为CCResult.defaultNullResult()
        if (result == null) {
            result = CCResult.defaultNullResult();
        }
        if (CC.VERBOSE_LOG) {
            CC.verboseLog(callId, "perform callback:" + cc.getCallback());
        }
        CCUtil.invokeCallback(cc, result);
        return result;
    }
}
