package com.billy.cc.core.component;

/**
 * 接口：最终调用组件类的句柄
 * @author billy.qi
 */
interface ICaller {
    /**
     * 取消
     */
    void cancel();

    /**
     * 超时
     */
    void timeout();

}
