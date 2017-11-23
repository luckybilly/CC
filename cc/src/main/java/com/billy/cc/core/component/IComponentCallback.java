package com.billy.cc.core.component;

/**
 * 组件回调
 * @author billy.qi
 * @since 17/6/29 11:34
 */
public interface IComponentCallback {
    /**
     * call when cc is received CCResult
     * @param cc cc
     * @param result the CCResult
     */
    void onResult(CC cc, CCResult result);
}
