package com.billy.cc.core.component;

import android.os.DeadObjectException;
import android.os.RemoteException;

import com.billy.cc.core.component.remote.IRemoteCCService;
import com.billy.cc.core.component.remote.IRemoteCallback;
import com.billy.cc.core.component.remote.RemoteCC;
import com.billy.cc.core.component.remote.RemoteCCResult;

import java.util.concurrent.ConcurrentHashMap;

/**
 * App内跨进程调用组件
 * @author billy.qi
 * @since 18/6/24 00:25
 */
class SubProcessCCInterceptor implements ICCInterceptor {

    private static final ConcurrentHashMap<String, IRemoteCCService> CONNECTIONS = new ConcurrentHashMap<>();

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class SubProcessCCInterceptorHolder {
        private static final SubProcessCCInterceptor INSTANCE = new SubProcessCCInterceptor();
    }
    SubProcessCCInterceptor(){}
    /** 获取SubProcessCCInterceptor的单例对象 */
    static SubProcessCCInterceptor getInstance() {
        return SubProcessCCInterceptorHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    @Override
    public CCResult intercept(Chain chain) {
        String componentName = chain.getCC().getComponentName();
        String processName = ComponentManager.getComponentProcessName(componentName);
        return multiProcessCall(chain, processName, CONNECTIONS);
    }

    CCResult multiProcessCall(Chain chain, String processName
            , ConcurrentHashMap<String, IRemoteCCService> connectionCache) {
        if (processName == null) {
            return CCResult.error(CCResult.CODE_ERROR_NO_COMPONENT_FOUND);
        }
        CC cc = chain.getCC();
        ProcessCrossTask task = new ProcessCrossTask(cc, processName, connectionCache);
        ComponentManager.threadPool(task);
        if (!cc.isFinished() && cc.resultRequired()) {
            //执行 Wait4ResultInterceptor
            chain.proceed();
            //如果是提前结束的，跨进程通知被调用方
            if (cc.isCanceled()) {
                task.cancel();
            } else if (cc.isTimeout()) {
                task.timeout();
            }
        } else {
            return CCResult.success();
        }
        return cc.getResult();
    }

    protected IRemoteCCService getMultiProcessService(String processName) {
        return RemoteCCService.get(processName);
    }

    class ProcessCrossTask implements Runnable {

        private final CC cc;
        private final String processName;
        private final ConcurrentHashMap<String, IRemoteCCService> connectionCache;
        private IRemoteCCService service;

        ProcessCrossTask(CC cc, String processName, ConcurrentHashMap<String, IRemoteCCService> connectionCache) {
            this.cc = cc;
            this.processName = processName;
            this.connectionCache = connectionCache;
        }

        @Override
        public void run() {
            RemoteCC processCrossCC = new RemoteCC(cc);
            call(processCrossCC);
        }

        private void call(RemoteCC processCrossCC) {
            try {
                service = connectionCache.get(processName);
                if (service == null) {
                    //app内部多进程
                    service = getMultiProcessService(processName);
                    if (service != null) {
                        connectionCache.put(processName, service);
                    }
                }
                if (cc.isFinished()) {
                    return;
                }
                if (service == null) {
                    setResult(CCResult.error(CCResult.CODE_ERROR_NO_COMPONENT_FOUND));
                    return;
                }
                service.call(processCrossCC, new IRemoteCallback.Stub() {
                    @Override
                    public void callback(RemoteCCResult remoteCCResult) throws RemoteException {
                        setResult(remoteCCResult.toCCResult());
                    }
                });
            } catch (DeadObjectException e) {
                connectionCache.remove(processName);
                call(processCrossCC);
            } catch (Exception e) {
                e.printStackTrace();
                setResult(CCResult.error(CCResult.CODE_ERROR_REMOTE_CC_DELIVERY_FAILED));
            }
        }

        void setResult(CCResult result) {
            if (cc.resultRequired()) {
                cc.setResult4Waiting(result);
            } else {
                cc.setResult(result);
            }
        }

        void cancel() {
            try {
                service.cancel(cc.getCallId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void timeout() {
            try {
                service.timeout(cc.getCallId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
