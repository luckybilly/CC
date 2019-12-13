package com.billy.cc.core.component;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 启动拦截器调用链
 * @author billy.qi
 */
class ChainProcessor implements Callable<CCResult> {

    private final Chain chain;

    ChainProcessor(Chain chain) {
        this.chain = chain;
    }

    @Override
    public CCResult call() throws Exception {
        CC cc = chain.getCC();
        String callId = cc.getCallId();
        //从开始调用的时候就开始进行监控，也许时间设置的很短，可能都不需要执行拦截器调用链
        CCMonitor.addMonitorFor(cc);
        CCResult result;
        try {
            if (CC.VERBOSE_LOG) {
                int poolSize = ((ThreadPoolExecutor) ComponentManager.CC_THREAD_POOL).getPoolSize();
                CC.verboseLog(callId, "process cc at thread:"
                        + Thread.currentThread().getName() + ", pool size=" + poolSize);
            }
            if (cc.isFinished()) {
                //timeout, cancel, CC.sendCCResult(callId, ccResult)
                result = cc.getResult();
            } else {
                try {
                    CC.verboseLog(callId, "start interceptor chain");
                    result = chain.proceed();
                    if (CC.VERBOSE_LOG) {
                        CC.verboseLog(callId, "end interceptor chain.CCResult:" + result);
                    }
                } catch(Exception e) {
                    result = CCResult.defaultExceptionResult(e);
                }
            }
        } catch(Exception e) {
            result = CCResult.defaultExceptionResult(e);
        } finally {
            CCMonitor.removeById(callId);
        }
        //返回的结果，永不为null，默认为CCResult.defaultNullResult()
        if (result == null) {
            result = CCResult.defaultNullResult();
        }
        //调用请求处理完成后，CC对象中不存储CCResult
        cc.setResult(null);
        performCallback(cc, result);
        return result;
    }

    private static void performCallback(CC cc, CCResult result) {
        IComponentCallback callback = cc.getCallback();
        if (CC.VERBOSE_LOG) {
            CC.verboseLog(cc.getCallId(), "perform callback:" + cc.getCallback()
                    + ", CCResult:" + result);
        }
        if (callback == null) {
            return;
        }
        if (cc.isCallbackOnMainThread()) {
            ComponentManager.mainThread(new CallbackRunnable(callback, cc, result));
        } else {
            try {
                callback.onResult(cc, result);
            } catch(Exception e) {
                CCUtil.printStackTrace(e);
            }
        }
    }
    private static class CallbackRunnable implements Runnable {
        private final CC cc;
        private IComponentCallback callback;
        private CCResult result;

        CallbackRunnable(IComponentCallback callback, CC cc, CCResult result) {
            this.cc = cc;
            this.callback = callback;
            this.result = result;
        }

        @Override
        public void run() {
            try {
                callback.onResult(cc, result);
            } catch(Exception e) {
                CCUtil.printStackTrace(e);
            }
        }
    }
}
