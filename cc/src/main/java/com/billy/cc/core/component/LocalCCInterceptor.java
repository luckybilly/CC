package com.billy.cc.core.component;

/**
 * 调用当前app内组件的拦截器<br>
 * 如果本地找不到该组件，则添加{@link RemoteCCInterceptor}来处理<br>
 * 如果组件onCall方法执行完之前未调用{@link CC#sendCCResult(String, CCResult)}方法，则按返回值来进行以下处理：<br>
 *  返回值为false: 回调状态码为 {@link CCResult#CODE_ERROR_CALLBACK_NOT_INVOKED} 的错误结果给调用方<br>
 *  返回值为true: 添加{@link Wait4ResultInterceptor}来等待组件调用{@link CC#sendCCResult(String, CCResult)}方法
 * @author billy.qi
 */
class LocalCCInterceptor implements ICCInterceptor {

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class LocalCCInterceptorHolder {
        private static final LocalCCInterceptor INSTANCE = new LocalCCInterceptor();
    }
    private LocalCCInterceptor (){}
    /** 获取LocalCCInterceptor的单例对象 */
    static LocalCCInterceptor getInstance() {
        return LocalCCInterceptorHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    @Override
    public CCResult intercept(Chain chain) {
        CC cc = chain.getCC();
        IComponent component = ComponentManager.getComponentByName(cc.getComponentName());
        if (component == null) {
            CC.verboseLog(cc.getCallId(), "component not found in this app. maybe 2 reasons:"
                    + "\n1. CC.enableRemoteCC changed to false"
                    + "\n2. Component named \"%s\" is a IDynamicComponent but now is unregistered"
            );
            return CCResult.error(CCResult.CODE_ERROR_NO_COMPONENT_FOUND);
        }
        //是否需要wait：异步调用且未设置回调，则不需要wait
        boolean callbackNecessary = !cc.isAsync() || cc.getCallback() != null;
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
