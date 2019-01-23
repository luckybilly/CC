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
        Boolean notFoundInCurApp = null;
        if (TextUtils.isEmpty(componentName)) {
            //没有指定要调用的组件名称，中止运行
            code = CCResult.CODE_ERROR_COMPONENT_NAME_EMPTY;
        } else if (cc.getContext() == null) {
            //context为null (没有设置context 且 CC中获取application失败)
            code = CCResult.CODE_ERROR_CONTEXT_NULL;
        } else {
            if (!ComponentManager.hasComponent(componentName)) {
                //当前进程中不包含此组件，查看一下其它进程中是否包含此组件
                notFoundInCurApp = TextUtils.isEmpty(ComponentManager.getComponentProcessName(componentName));
                if (notFoundInCurApp && !CC.isRemoteCCEnabled()) {
                    //本app内所有进程均没有指定的组件，并且设置了不会调用外部app的组件
                    code = CCResult.CODE_ERROR_NO_COMPONENT_FOUND;
                    CC.verboseLog(cc.getCallId(),"componentName=" + componentName
                            + " is not exists and CC.enableRemoteCC is " + CC.isRemoteCCEnabled());
                }
            }
        }
        if (code != 0) {
            return CCResult.error(code);
        }
        //执行完自定义拦截器，并且通过有效性校验后，再确定具体调用组件的方式
        if (ComponentManager.hasComponent(componentName)) {
            //调用当前进程中的组件
            chain.addInterceptor(LocalCCInterceptor.getInstance());
        } else {
            if (notFoundInCurApp == null) {
                notFoundInCurApp = TextUtils.isEmpty(ComponentManager.getComponentProcessName(componentName));
            }
            if (notFoundInCurApp) {
                //调用设备上安装的其它app（组件单独运行的app）中的组件
                chain.addInterceptor(RemoteCCInterceptor.getInstance());
            } else {
                //调用app内部子进程中的组件
                chain.addInterceptor(SubProcessCCInterceptor.getInstance());
            }
        }
        chain.addInterceptor(Wait4ResultInterceptor.getInstance());
        // 执行上面添加的拦截器，开始执行组件调用
        return chain.proceed();
    }
}
