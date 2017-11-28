package com.billy.cc.core.component;

/**
 * 等待异步调用CC.sendCCResult(callId, ccResult)
 * @author billy.qi
 */
class Wait4ResultInterceptor implements ICCInterceptor {

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class Wait4ResultInterceptorHolder {
        private static final Wait4ResultInterceptor INSTANCE = new Wait4ResultInterceptor();
    }
    private Wait4ResultInterceptor (){}
    /** 获取Wait4ResultInterceptor的单例对象 */
    static Wait4ResultInterceptor getInstance() {
        return Wait4ResultInterceptorHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    @Override
    public CCResult intercept(Chain chain) {
        CC cc = chain.getCC();
        String callId = cc.getCallId();
        //等待调用CC.sendCCResult(callId, result)
        synchronized (cc.wait4resultLock) {
            if (!cc.isFinished()) {
                try {
                    CC.verboseLog(callId, "start waiting for CC.sendCCResult(...)");
                    cc.wait4resultLock.wait();
                    CC.verboseLog(callId, "end waiting for CC.sendCCResult(...)");
                } catch (InterruptedException ignored) {
                }
            }
        }
        return cc.getResult();
    }
}
