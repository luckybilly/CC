package com.billy.cc.core.component;

import android.text.TextUtils;

/**
 * 转发组件调用 <br>
 * 注：如果需要做成全局拦截器，需要额外实现 {@link IGlobalCCInterceptor}接口
 * @author billy.qi
 * @since 18/9/2 13:40
 */
public abstract class BaseForwardInterceptor implements ICCInterceptor {
    @Override
    public CCResult intercept(Chain chain) {
        CC cc = chain.getCC();
        String forwardComponentName = shouldForwardCC(cc, cc.getComponentName());
        if (!TextUtils.isEmpty(forwardComponentName)) {
            cc.forwardTo(forwardComponentName);
        }
        return chain.proceed();
    }

    /**
     * 根据当前组件调用对象获取需要转发到的组件名称
     * @param cc 当前组件调用对象
     * @param componentName 当前调用的组件名称
     * @return 转发的目标组件名称（为null则不执行转发）
     */
    protected abstract String shouldForwardCC(CC cc, String componentName);
}
