package com.billy.cc.core.component;

/**
 * 调用当前app内组件的拦截器
 * @author billy.qi
 */
class LocalCCInterceptor implements ICCInterceptor {

    private final IComponent component;


    LocalCCInterceptor(IComponent component) {
        this.component = component;
    }

    @Override
    public CCResult intercept(Chain chain) {
        CC cc = chain.getCC();
        //是否需要wait：异步调用且未设置回调，则不需要wait
        boolean callbackNecessary = !cc.isAsync() || cc.getCallback() != null;
        CCResult result = null;
        try {
            String callId = cc.getCallId();
            boolean callbackDelay = component.onCall(cc);
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, component.getName() + ":"
                        + component.getClass().getName()
                        + ".onCall(cc) return:" + callbackDelay
                        );
            }
            //兼容异步调用时等待回调结果（同步调用时，此时CC.sendCCResult(callId, result)方法已调用）
            if (!cc.isFinished() && callbackNecessary) {
                //component.onCall(cc)没报exception并且指定了要延时回调结果才进入正常wait流程
                if (callbackDelay) {
                    chain.addInterceptor(Wait4ResultInterceptor.getInstance());
                    return chain.proceed();
                } else {
                    CC.logError("component.onCall(cc) return false but CC.sendCCResult(...) not called!"
                            + "\nmaybe: actionName error"
                            + "\nor if-else not call CC.sendCCResult"
                            + "\nor switch-case-default not call CC.sendCCResult"
                            + "\nor try-catch block not call CC.sendCCResult."
                    );
                    //没有返回结果，且不是延时回调（也就是说不会收到结果了）
                    return CCResult.error(CCResult.CODE_ERROR_CALLBACK_NOT_INVOKED);
                }
            }
        } catch(Exception e) {
            return CCResult.defaultExceptionResult(e);
        }
        return cc.getResult();
    }

}
