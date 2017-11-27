package com.billy.cc.core.component;

import android.text.TextUtils;

/**
 * 检查cc是否合法
 * @author billy.qi
 */
class ValidateInterceptor implements ICCInterceptor {

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class ValidateInterceptorHolder {
        private static final ValidateInterceptor INSTANCE = new ValidateInterceptor();
    }
    private ValidateInterceptor (){}
    /** 获取ValidateInterceptor的单例对象 */
    static ValidateInterceptor getInstance() {
        return ValidateInterceptorHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    @Override
    public CCResult intercept(Chain chain) {
        CC cc = chain.getCC();
        String componentName = cc.getComponentName();
        int code = 0;
        if (TextUtils.isEmpty(componentName)) {
            //没有指定要调用的组件名称，中止运行
            code = CCResult.CODE_ERROR_COMPONENT_NAME_EMPTY;
        } else if (cc.getContext() == null) {
            //context为null (没有设置context 且 CC中获取application失败)
            code = CCResult.CODE_ERROR_CONTEXT_NULL;
        } else {
            boolean hasComponent = ComponentManager.hasComponent(componentName);
            if (!hasComponent && !CC.CALL_REMOTE_CC_IF_NEED) {
                //本app内没有改组件，并且设置了不会调用外部app的组件
                code = CCResult.CODE_ERROR_NO_COMPONENT_FOUND;
                CC.verboseLog(cc.getCallId(),"componentName=" + componentName
                        + " is not exists and CC.enableRemoteCC is " + CC.CALL_REMOTE_CC_IF_NEED);
            }
        }
        if (code != 0) {
            return CCResult.error(code);
        }
        return chain.proceed();
    }
}
