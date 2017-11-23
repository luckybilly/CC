package com.billy.cc.core.component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 调用当前app内组件的拦截器
 * @author billy.qi
 */
class LocalCCInterceptor implements ICCInterceptor, ICaller {

    static final ConcurrentHashMap<String, LocalCCInterceptor> RESULT_RECEIVER = new ConcurrentHashMap<>();
    private final byte[] lock = new byte[0];
    private final IComponent component;
    final CC cc;
    private AtomicBoolean resultReceived = new AtomicBoolean(false);
    private CCResult result;


    LocalCCInterceptor(IComponent component, CC cc) {
        this.component = component;
        this.cc = cc;
        //兼容同步与异步两种需求
        RESULT_RECEIVER.put(cc.getCallId(), this);
        this.cc.setCaller(this);
    }

    @Override
    public CCResult intercept(Chain chain) {
        //是否需要wait：异步调用且未设置回调，则不需要wait
        boolean callbackNecessary = !cc.isAsync() || cc.getCallback() != null;
        try {
            //未被cancel
            if (!resultReceived.get()) {
                String callId = cc.getCallId();
                boolean callbackDelay = component.onCall(cc);
                if (CC.VERBOSE_LOG) {
                    CC.verboseLog(callId, component.getName() + ":"
                            + component.getClass().getName()
                            + ".onCall(cc) return:" + callbackDelay
                            + ", resultReceived=" + resultReceived);
                }
                //兼容异步调用时等待回调结果（同步调用时，此时receiveCCResult(result)方法已调用）
                if (!resultReceived.get() && callbackNecessary) {
                    //component.onCall(cc)没报exception并且指定了要延时回调结果才进入正常wait流程
                    if (callbackDelay) {
                        if (CC.VERBOSE_LOG) {
                            CC.verboseLog(callId, "start waiting for CC.sendCCResult(\""
                                    + callId + "\", ccResult)");
                        }
                        //等待组件实现方调用CC.invokeCallback(callId, result)
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException ignored) {
                            }
                        }
                        if (CC.VERBOSE_LOG) {
                            CC.verboseLog(callId, "end waiting for CC.sendCCResult(\"" + callId
                                    + "\", ccResult), resultReceived=" + resultReceived);
                        }
                    } else {
                        //没有返回结果，且不是延时回调（也就是说不会收到结果了）
                        result = CCResult.error(CCResult.CODE_ERROR_CALLBACK_NOT_INVOKED);
                        CC.logError("component.onCall(cc) return false but CC.sendCCResult(...) not called!"
                                + "\nmaybe: actionName error"
                                + "\nor if-else not call CC.sendCCResult"
                                + "\nor switch-case-default not call CC.sendCCResult"
                                + "\nor try-catch block not call CC.sendCCResult."
                        );
                    }
                }
            }
        } catch(Exception e) {
            result = CCResult.defaultExceptionResult(e);
        }
        cc.setCaller(null);
        return result;
    }

    void receiveCCResult(CCResult ccResult) {
        if (!resultReceived.compareAndSet(false,true)) {
            return;
        }
        RESULT_RECEIVER.remove(this.cc.getCallId());
        synchronized (lock) {
            if (ccResult != null) {
                result = ccResult;
            } else {
                result = CCResult.defaultNullResult();
            }
            lock.notifyAll();
        }
    }

    @Override
    public void cancel() {
        if (resultReceived.get()) {
            return;
        }
        if (CC.VERBOSE_LOG) {
            CC.verboseLog(cc.getCallId(), "LocalCC stopped (cancel).");
        }
        receiveCCResult(CCResult.error(CCResult.CODE_ERROR_CANCELED));
    }

    @Override
    public void timeout() {
        if (resultReceived.get()) {
            return;
        }
        if (CC.VERBOSE_LOG) {
            CC.verboseLog(cc.getCallId(), "LocalCC stopped (timeout).");
        }
        receiveCCResult(CCResult.error(CCResult.CODE_ERROR_TIMEOUT));
    }
}
