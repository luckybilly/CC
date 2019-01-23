package com.billy.cc.core.component;

import android.os.Looper;

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
        try {
            String callId = cc.getCallId();
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "start component:%s, cc: %s", component.getClass().getName(), cc.toString());
            }
            boolean shouldSwitchThread = false;
            LocalCCRunnable runnable = new LocalCCRunnable(cc, component);
            if (component instanceof IMainThread) {
                //当前是否在主线程
                boolean curIsMainThread = Looper.myLooper() == Looper.getMainLooper();
                //该action是否应该在主线程运行
                Boolean runOnMainThread = ((IMainThread) component).shouldActionRunOnMainThread(cc.getActionName(), cc);
                //是否需要切换线程执行 component.onCall(cc) 方法
                shouldSwitchThread = runOnMainThread != null && runOnMainThread ^ curIsMainThread;
                if (shouldSwitchThread) {
                    runnable.setShouldSwitchThread(true);
                    if (runOnMainThread) {
                        //需要在主线程运行，但是当前线程不是主线程
                        ComponentManager.mainThread(runnable);
                    } else {
                        //需要在子线程运行，但当前线程不是子线程
                        ComponentManager.threadPool(runnable);
                    }
                }
            }
            if (!shouldSwitchThread) {
                //不需要切换线程，直接运行
                runnable.run();
            }
            //兼容以下情况：
            //  1. 不需要切换线程，但需要等待异步实现调用CC.sendCCResult(...)
            //  2. 需要切换线程，等待切换后的线程调用组件后调用CC.sendCCResult(...)
            if (!cc.isFinished()) {
                chain.proceed();
            }
        } catch(Exception e) {
            return CCResult.defaultExceptionResult(e);
        }
        return cc.getResult();
    }


    static class LocalCCRunnable implements Runnable {
        private final String callId;
        private CC cc;
        private IComponent component;
        private boolean shouldSwitchThread;

        LocalCCRunnable(CC cc, IComponent component) {
            this.cc = cc;
            this.callId = cc.getCallId();
            this.component = component;
        }

        void setShouldSwitchThread(boolean shouldSwitchThread) {
            this.shouldSwitchThread = shouldSwitchThread;
        }

        @Override
        public void run() {
            if (cc.isFinished()) {
                return;
            }
            try {
                boolean callbackDelay = component.onCall(cc);
                if (CC.VERBOSE_LOG) {
                    CC.verboseLog(callId, component.getName() + ":"
                            + component.getClass().getName()
                            + ".onCall(cc) return:" + callbackDelay
                    );
                }
                if (!callbackDelay && !cc.isFinished()) {
                    CC.logError("component.onCall(cc) return false but CC.sendCCResult(...) not called!"
                            + "\nmaybe: actionName error"
                            + "\nor if-else not call CC.sendCCResult"
                            + "\nor switch-case-default not call CC.sendCCResult"
                            + "\nor try-catch block not call CC.sendCCResult."
                    );
                    //没有返回结果，且不是延时回调（也就是说不会收到结果了）
                    setResult(CCResult.error(CCResult.CODE_ERROR_CALLBACK_NOT_INVOKED));
                }
            } catch(Exception e) {
                setResult(CCResult.defaultExceptionResult(e));
            }
        }

        private void setResult(CCResult result) {
            if (shouldSwitchThread) {
                //若出现线程切换
                // LocalCCInterceptor.intercept会执行chain.proceed()进入wait状态
                // 需要解除wait状态
                cc.setResult4Waiting(result);
            } else {
                cc.setResult(result);
            }
        }
    }
}
