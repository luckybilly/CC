package com.billy.cc.core.component;

/**
 * 指定是否在主线程运行
 * @author billy.qi
 * @since 18/9/19 11:31
 */
public interface IMainThread {
    /**
     * 根据当前actionName确定组件的{@link IComponent#onCall(CC)} 方法是否在主线程运行
     * @param actionName 当前CC的action名称
     * @param cc 当前CC对象
     * @return 3种返回值: <br>
     *              null:默认状态，不固定运行的线程（在主线程同步调用时在主线程运行，其它情况下在子线程运行）<br>
     *              true:固定在主线程运行<br>
     *              false:固定子线程运行<br>
     */
    Boolean shouldActionRunOnMainThread(String actionName, CC cc);
}
